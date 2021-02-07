package kiosk

import kiosk.encoding.ScalaErgoConverters.{ergoTreeToString, groupElementToString, stringToErgoTree, stringToGroupElement}
import kiosk.ergo.{DhtData, KioskBox, KioskType, groupElementToKioskGroupElement}
import org.sh.reflect.DefaultTypeHandler
import play.api.libs.json.{JsString, JsValue, Json, Writes}
import sigmastate.Values.ErgoTree
import special.sigma.GroupElement

object EasyWebFormatters {
  implicit val writesGroupElement: Writes[GroupElement] = new Writes[GroupElement] {
    override def writes(o: GroupElement): JsValue = new JsString(o.hex)
  }
  implicit val writesDhtData = Json.writes[DhtData]

  implicit val writesKioskType = new Writes[KioskType[_]] {
    override def writes(o: KioskType[_]): JsValue = JsString(o.toString)
  }
  implicit val writesKioskBox = Json.writes[KioskBox]

  DefaultTypeHandler.addType[DhtData](classOf[DhtData], _ => ???, dhtData => Json.prettyPrint(Json.toJson(dhtData)))
  DefaultTypeHandler.addType[KioskBox](classOf[KioskBox], _ => ???, kioskBox => Json.prettyPrint(Json.toJson(kioskBox)))
  DefaultTypeHandler.addType[GroupElement](classOf[GroupElement], stringToGroupElement, groupElementToString)
  DefaultTypeHandler.addType[ErgoTree](classOf[ErgoTree], stringToErgoTree, ergoTreeToString)

}
