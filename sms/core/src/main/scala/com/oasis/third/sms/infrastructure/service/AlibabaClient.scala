package com.oasis.third.sms.infrastructure.service

import com.aliyuncs.DefaultAcsClient
import com.aliyuncs.dysmsapi.model.v20170525.SendSmsRequest
import com.aliyuncs.http.MethodType
import com.aliyuncs.profile.DefaultProfile
import io.circe.Printer
import io.circe.syntax._
import org.ryze.micro.core.tool.ConfigLoader
import org.slf4j.LoggerFactory

class AlibabaClient
{
  private[this] val log = LoggerFactory.getLogger(classOf[AlibabaClient])

  //发送短信
  private[this] def send(mobile: String)(templateId: String)(params: Map[String, String] = Map.empty) =
  {
    //设置超时时间-可自行调整
    System setProperty ("sun.net.client.defaultConnectTimeout", "10000")
    System setProperty ("sun.net.client.defaultReadTimeout", "10000")
    //初始化ascClient需要的几个参数
    val product         = "Dysmsapi"
    //短信API产品名称（短信产品名固定，无需修改）
    val domain          = "dysmsapi.aliyuncs.com"
    //短信API产品域名（接口地址固定，无需修改）
    //替换成你的AK
    val accessKeyId     = AlibabaClient.accessKey
    //你的accessKeyId,参考本文档步骤2
    val accessKeySecret = AlibabaClient.accessSecret
    //你的accessKeySecret，参考本文档步骤2
    //初始化ascClient,暂时不支持多region（请勿修改）
    val profile         = DefaultProfile getProfile ("cn-hangzhou", accessKeyId, accessKeySecret)
    DefaultProfile addEndpoint ("cn-hangzhou", "cn-hangzhou", product, domain)
    val acsClient       = new DefaultAcsClient(profile)
    //组装请求对象
    val request         = new SendSmsRequest
    //使用post提交
    request setMethod MethodType.POST
    //必填:待发送手机号。支持以逗号分隔的形式进行批量调用，批量上限为1000个手机号码,批量调用相对于单条调用及时性稍有延迟,验证码类型的短信推荐使用单条调用的方式
    request setPhoneNumbers mobile
    //必填:短信签名-可在短信控制台中找到
    request setSignName "泓华医疗"
    //必填:短信模板-可在短信控制台中找到
    request setTemplateCode templateId
    //可选:模板中的变量替换JSON串,如模板内容为"亲爱的${name},您的验证码为${code}"时,此处的值为
    //友情提示:如果JSON中需要带换行符,请参照标准的JSON协议对换行符的要求,比如短信内容中包含\r\n的情况在JSON中需要表示成\\r\\n,否则会导致JSON在服务端解析失败
    val data            = params.asJson.pretty(Printer.spaces2)
    log info s"data: $data"
    request setTemplateParam data
    //请求失败这里会抛ClientException异常
    val sendSmsResponse = acsClient getAcsResponse request
    //请求失败这里会抛ClientException异常
    acsClient getAcsResponse request match
    {
      //请求成功
      case d if "OK" == d.getCode => d.getRequestId
      case d                      =>
      {
        log info s"code     : ${d.getCode}"
        log info s"message  : ${d.getMessage}"
        log info s"requestId: ${d.getRequestId}"
        d.getRequestId
      }
    }
  }

  /**
    * 达人通知
    */
  def notice(mobile: String) = send(mobile)(AlibabaClient.notice)()
  /**
    * 验证码
    */
  def captcha(mobile: String)(code: String) = send(mobile)(AlibabaClient.captcha)(Map("code" -> code))
  /**
    * 支付码
    */
  def payment(mobile: String)(code: String) = send(mobile)(AlibabaClient.payment)(Map("code" -> code))
}

object AlibabaClient extends ConfigLoader
{
  //短信
  private[this] val alibabaConfig  = loader.getConfig("alibaba")
  //短信模块
  private[this] val templateConfig = alibabaConfig.getConfig("template")

  lazy val accessKey    = alibabaConfig.getString("access-key")
  lazy val accessSecret = alibabaConfig.getString("access-secret")
  lazy val captcha      = templateConfig.getString("captcha")
  lazy val payment      = templateConfig.getString("payment")
  lazy val notice       = templateConfig.getString("notice")
}