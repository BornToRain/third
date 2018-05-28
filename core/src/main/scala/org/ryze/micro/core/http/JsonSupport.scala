package org.ryze.micro.core.http

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.{ContentType, ContentTypeRange, HttpEntity}
import akka.http.scaladsl.model.MediaTypes.`application/json`
import akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshaller}
import akka.util.ByteString
import io.circe._
import io.circe.generic.extras.{AutoDerivation, Configuration}

/**
  * Json自动Encode、Decode支持
  */
trait JsonSupport extends CirceSupport with AutoDerivation
{
  override implicit val printer       = Printer.noSpaces copy (dropNullValues = true)
  override implicit val configuration = Configuration.default.withDefaults.withDiscriminator("type")
}

/**
  * TypelevelCirceJSON解析支持
  */
trait CirceSupport
{
  //请求头
  val mediaTypes             = Seq(`application/json`)
  //解码请求头
  val unmarshallerMediaTypes = mediaTypes map ContentTypeRange.apply

  //JSON输出
  implicit val printer      : Printer
  //JSON配置
  implicit val configuration: Configuration

  /**
    * Json => HttpEntity
    */
  @inline
  implicit def jsonMarshaller: ToEntityMarshaller[Json] = Marshaller.oneOf(mediaTypes: _*)
  {
    `type` => Marshaller.withFixedContentType(ContentType(`type`))(json => HttpEntity(`type`, printer pretty json))
  }
  /**
    * T => HttpEntity
    */
  implicit def marshaller[T : Encoder]: ToEntityMarshaller[T] = jsonMarshaller compose Encoder[T].apply
  /**
    * HttpEntity => Json
    */
  @inline
  implicit def jsonUnmarshaller: FromEntityUnmarshaller[Json] = Unmarshaller.byteStringUnmarshaller
  .forContentTypes(unmarshallerMediaTypes: _*) map
  {
    case ByteString.empty => throw Unmarshaller.NoContentException
    case d                => (jawn parseByteBuffer d.asByteBuffer) fold (throw _, identity)
  }
  /**
    * HttpEntity => T
    */
  implicit def unmarshaller[T : Decoder]: FromEntityUnmarshaller[T] =
  {
    def decode(json: Json) = (Decoder[T] decodeJson json) fold (throw _, identity)
    jsonUnmarshaller map decode
  }
}

