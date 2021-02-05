package kiosk.script

import kiosk.encoding.ScalaErgoConverters
import org.ergoplatform.Pay2SAddress
import org.sh.easyweb.Text
import org.sh.reflect.DataStructures.EasyMirrorSession
import sigmastate.Values.ErgoTree
import sigmastate.serialization.ErgoTreeSerializer.DefaultSerializer

class KioskScriptCreator(val $env: KioskScriptEnv) extends EasyMirrorSession {

  def getScriptHash(ergoScript: Text): Array[Byte] = {
    val $INFO$ = "Outputs the blake2b256 hash of the ErgoTree corresponding to ergoScript"
    val $ergoScript$ : String = """{
  sigmaProp(1 < 2)
}"""

    scorex.crypto.hash.Blake2b256(getErgoTree(ergoScript)).toArray
  }

  def getAddressHash(address: String): Array[Byte] = {
    val $INFO$ = "Outputs the blake2b256 hash of the ErgoTree corresponding to address"
    val scriptBytes = ScalaErgoConverters.getAddressFromString(address).script.bytes

    scorex.crypto.hash.Blake2b256(scriptBytes).toArray
  }

  def getAddress(ergoTree: String): String = {
    val $INFO$ = "Outputs the address corresponding to (hex encoded) ergoTree"
    val ergoTreeBytes = ScalaErgoConverters.stringToErgoTree(ergoTree)
    val address = ScalaErgoConverters.getAddressFromErgoTree(ergoTreeBytes)
    ScalaErgoConverters.getStringFromAddress(address)
  }

  def getErgoTree(ergoScript: Text): Array[Byte] = {
    val $INFO$ = "Outputs the ErgoTree corresponding to ergoScript. This is the output of box.propositionBytes"
    val $ergoScript$ : String = """{
  sigmaProp(1 < 2)
}"""

    val ergoTree = $compile(ergoScript.getText)
    DefaultSerializer.serializeErgoTree(ergoTree)
  }

  def getP2SAddress(ergoScript: Text) = {
    val $ergoScript$ = """{
  sigmaProp(1 < 2)
}"""
    import ScriptUtil.ergoAddressEncoder
    Pay2SAddress($compile(ergoScript.getText)).toString
  }

  def $compile(ergoScript: String): ErgoTree = {
    ScriptUtil.compile($env.$envMap.toMap, ergoScript)
  }

  override def $setSession(sessionSecret: Option[String]): KioskScriptCreator = new KioskScriptCreator($env.$setSession(sessionSecret))
}
