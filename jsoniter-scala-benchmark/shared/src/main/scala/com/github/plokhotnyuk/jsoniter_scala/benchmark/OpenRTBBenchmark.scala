package com.github.plokhotnyuk.jsoniter_scala.benchmark

import java.nio.charset.StandardCharsets.UTF_8
import com.github.plokhotnyuk.jsoniter_scala.benchmark.JsoniterScalaCodecs._
import com.github.plokhotnyuk.jsoniter_scala.core._
import org.openjdk.jmh.annotations.Setup

abstract class OpenRTBBenchmark extends CommonParams {
  var obj: OpenRTB.BidRequest = _
  var jsonBytes: Array[Byte] = _
  var preallocatedBuf: Array[Byte] = _
  var jsonString1: String = """{"id":"b5ba5ed2-547e-4e86-8a84-34a440dad6db","imp":[{"id":"1","metric":[{"type":"viewability","value":0.85}],"banner":{"w":728,"h":90,"btype":[4],"battr":[14],"pos":1,"mimes":["image/jpeg","image/png","image/gif"],"api":[3]},"pmp":{"private_auction":1,"deals":[{"id":"1452f.eadb4.7aaa","bidfloor":5.3,"at":1,"wseat":["45","165","33"]}]},"tagid":"agltb3B1Yi1pbmNyDQsSBFNpdGUY7fD0FAw","bidfloor":0.5}],"app":{"id":"agltb3B1Yi1pbmNyDAsSA0FwcBiJkfIUDA","name":"Yahoo Weather","bundle":"628677149","storeurl":"https://itunes.apple.com/id628677149","cat":["weather","IAB15","IAB15-10"],"ver":"1.0.2","publisher":{"id":"agltb3B1Yi1pbmNyDAsSA0FwcBiJkfTUCV","name":"yahoo","domain":"www.yahoo.com"}},"device":{"ua":"Mozilla/5.0 (iPhone; CPU iPhone OS 6_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9A334 Safari/7534.48.3","geo":{"lat":35.012345,"lon":-115.12345,"country":"USA","region":"CA","metro":"803","city":"Los Angeles","zip":"90049"},"dnt":0,"ip":"123.145.167.189","devicetype":1,"make":"Apple","model":"iPhone","os":"iOS","osv":"6.1","js":1,"language":"en","carrier":"VERIZON","mccmnc":"310-005","connectiontype":3,"ifa":"30255BCE-4CDA-4F62-91DC-4758FDFF8512","dpidsha1":"CCF6DC12B98AEB2346AFE1BEE7860DF01FDE158B","dpidmd5":"93D05D4D69DEE2BC6645D9F0A0C1938C"},"user":{"id":"ffffffd5135596709273b3a1a07e466ea2bf4fff","yob":1984,"gender":"M"},"tmax":120,"bcat":["IAB25","IAB7-39","IAB8-18","IAB8-5","IAB9-9"],"badv":["apple.com","go-text.me","heywire.com"],"reqs":{"coppa":0}}"""
  var jsonString2: String = """{"app":{"bundle":"628677149","cat":["weather","IAB15","IAB15-10"],"id":"agltb3B1Yi1pbmNyDAsSA0FwcBiJkfIUDA","name":"Yahoo Weather","publisher":{"domain":"www.yahoo.com","id":"agltb3B1Yi1pbmNyDAsSA0FwcBiJkfTUCV","name":"yahoo"},"storeurl":"https://itunes.apple.com/id628677149","ver":"1.0.2"},"badv":["apple.com","go-text.me","heywire.com"],"bcat":["IAB25","IAB7-39","IAB8-18","IAB8-5","IAB9-9"],"device":{"carrier":"VERIZON","connectiontype":3,"devicetype":1,"dnt":0,"dpidmd5":"93D05D4D69DEE2BC6645D9F0A0C1938C","dpidsha1":"CCF6DC12B98AEB2346AFE1BEE7860DF01FDE158B","geo":{"city":"Los Angeles","country":"USA","lat":35.012345,"lon":-115.12345,"metro":"803","region":"CA","zip":"90049"},"ifa":"30255BCE-4CDA-4F62-91DC-4758FDFF8512","ip":"123.145.167.189","js":1,"language":"en","make":"Apple","mccmnc":"310-005","model":"iPhone","os":"iOS","osv":"6.1","ua":"Mozilla/5.0 (iPhone; CPU iPhone OS 6_1 like Mac OS X) AppleWebKit/534.46 (KHTML, like Gecko) Version/5.1 Mobile/9A334 Safari/7534.48.3"},"id":"b5ba5ed2-547e-4e86-8a84-34a440dad6db","imp":[{"banner":{"api":[3],"battr":[14],"btype":[4],"h":90,"mimes":["image/jpeg","image/png","image/gif"],"pos":1,"w":728},"bidfloor":0.5,"id":"1","metric":[{"type":"viewability","value":0.85}],"pmp":{"deals":[{"at":1,"bidfloor":5.3,"id":"1452f.eadb4.7aaa","wseat":["45","165","33"]}],"private_auction":1},"tagid":"agltb3B1Yi1pbmNyDQsSBFNpdGUY7fD0FAw"}],"reqs":{"coppa":0},"tmax":120,"user":{"gender":"M","id":"ffffffd5135596709273b3a1a07e466ea2bf4fff","yob":1984}}"""

  @Setup
  def setup(): Unit = {
    jsonBytes = jsonString1.getBytes(UTF_8)
    obj = readFromArray[OpenRTB.BidRequest](jsonBytes)
    preallocatedBuf = new Array(jsonBytes.length + 128 /*to avoid possible out-of-bounds error*/)
  }
}