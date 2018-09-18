package io.suggest.jd.tags.qd

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.09.17 22:26
  * Description: Scripting modes model.
  */
object MQdScripts extends StringEnum[MQdScript] {

  case object Super extends MQdScript("p") {
    override def quillName = "super"
  }

  case object Sub extends MQdScript("b") {
    override def quillName = "sub"
  }

  override val values = findValues

  def withQuillNameOpt(quillName: String): Option[MQdScript] = {
    values.find(_.quillName == quillName)
  }

}


sealed abstract class MQdScript(override val value: String) extends StringEnumEntry {

  def quillName: String

  override final def toString = quillName

}

object MQdScript {

  implicit val FORMAT: Format[MQdScript] = {
    EnumeratumUtil.valueEnumEntryFormat( MQdScripts )
  }

  @inline implicit def univEq: UnivEq[MQdScript] = UnivEq.derive

}

