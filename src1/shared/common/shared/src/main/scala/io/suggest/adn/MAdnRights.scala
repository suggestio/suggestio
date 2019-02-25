package io.suggest.adn

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.08.17 13:19
  * Description: Модель ADN-прав. В основном она не очень актуальна, но всё же ещё используется.
  */
object MAdnRight {

  implicit val MADN_RIGHT_FORMAT: Format[MAdnRight] =
    EnumeratumUtil.valueEnumEntryFormat( MAdnRights )

  @inline implicit def univEq: UnivEq[MAdnRight] = UnivEq.derive

}

/** Класс элемента модели [[MAdnRights]]. */
sealed abstract class MAdnRight(override val value: String) extends StringEnumEntry {

  def longName: String

}


/** Положение участника сети и его возможности описываются флагами прав доступа. */
object MAdnRights extends StringEnum[MAdnRight] {

  /** Продьюсер может создавать свою рекламу. */
  case object PRODUCER extends MAdnRight("p") {
    override def longName = "producer"
  }

  /** Ресивер может отображать в выдаче и просматривать в ЛК рекламу других участников, которые транслируют свою
    * рекламу ему через receivers. Ресивер также может приглашать новых участников. */
  case object RECEIVER extends MAdnRight("r") {
    override def longName: String = "receiver"
  }


  override val values = findValues

}
