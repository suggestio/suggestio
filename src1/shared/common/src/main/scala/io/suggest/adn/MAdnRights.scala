package io.suggest.adn

import enumeratum._
import io.suggest.enum2.EnumeratumUtil
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 22.08.17 13:19
  * Description: Модель ADN-прав. В основном она не очень актуальна, но всё же ещё используется.
  */
object MAdnRight {

  implicit val MADN_RIGHT_FORMAT: Format[MAdnRight] = {
    EnumeratumUtil.enumEntryFormat( MAdnRights )
  }

}

/** Класс элемента модели [[MAdnRights]]. */
sealed abstract class MAdnRight extends EnumEntry {

  def name: String
  def longName: String

  override def entryName = name

}


/** Положение участника сети и его возможности описываются флагами прав доступа. */
object MAdnRights extends Enum[MAdnRight] {

  /** Продьюсер может создавать свою рекламу. */
  case object PRODUCER extends MAdnRight {
    override def name = "p"
    override def longName = "producer"
  }

  /** Ресивер может отображать в выдаче и просматривать в ЛК рекламу других участников, которые транслируют свою
    * рекламу ему через receivers. Ресивер также может приглашать новых участников. */
  case object RECEIVER extends MAdnRight {
    override def name: String = "r"
    override def longName: String = "receiver"
  }


  override val values = findValues

}
