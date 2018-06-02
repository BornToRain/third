package com.oasis.third.wechat.infrastructure.tool

import java.io.Writer

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.converters.{Converter, MarshallingContext, UnmarshallingContext}
import com.thoughtworks.xstream.converters.collections.AbstractCollectionConverter
import com.thoughtworks.xstream.core.util.QuickWriter
import com.thoughtworks.xstream.io.{HierarchicalStreamReader, HierarchicalStreamWriter}
import com.thoughtworks.xstream.io.xml.{PrettyPrintWriter, XppDriver}
import com.thoughtworks.xstream.mapper.Mapper

object XMLTool
{
  lazy val xStream = new XStream(new XppDriver()
  {
    override def createWriter(out: Writer) = new PrettyPrintWriter(out)
    {
      //双下划线问题
      override def encodeNode(name: String) = name
      //所有XML节点转换都增加CDATA标记
      override def writeText(writer: QuickWriter, text: String): Unit = writer write s"<![CDATA[$text]]>"
    }
  })
  xStream autodetectAnnotations true
  xStream registerConverter new ScalaOptionConverter
  xStream registerConverter new ScalaSeqConverter(xStream.getMapper)
  xStream aliasSystemAttribute (null, "class")

  @inline
  def toXML(data: AnyRef) =
  {
    xStream alias ("xml", data.getClass)
    xStream toXML data
  }

  @inline
  def fromXML(s: String) =
  {
    xStream alias("xml", classOf[String])
    xStream fromXML s
  }
}

/**
  * ScalaOption解析
  */
class ScalaOptionConverter extends Converter
{
  override def unmarshal(reader: HierarchicalStreamReader, context: UnmarshallingContext) = Option(reader.getValue)
  override def marshal(source: scala.Any, writer: HierarchicalStreamWriter, context: MarshallingContext): Unit =
  {
    val opt = source.asInstanceOf[Option[_]]
    context convertAnother opt.get
  }
  override def canConvert(`type`: Class[_]) = classOf[Some[_]].isAssignableFrom(`type`) || `type`.isAssignableFrom(None.getClass)
}

/**
  * ScalaSeq解析
  */
class ScalaSeqConverter(_mapper: Mapper) extends AbstractCollectionConverter(_mapper)
{
  override def canConvert(`type`: Class[_]) = classOf[:: [_]] == `type`
  override def unmarshal(reader: HierarchicalStreamReader,context: UnmarshallingContext) =
  {
    var xs: List[_] = Nil
    while (reader.hasMoreChildren)
    {
      reader.moveDown
      val item = readItem(reader, context, xs)
      xs = xs ++ List(item)
      reader.moveUp
    }
    xs
  }
  override def marshal(source: scala.Any, writer: HierarchicalStreamWriter, context: MarshallingContext) =
  {
    val xs = source.asInstanceOf[List[_]]
    xs foreach (writeItem(_, context, writer))
  }
}