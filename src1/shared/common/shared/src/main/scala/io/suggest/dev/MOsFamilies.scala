package io.suggest.dev

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import io.suggest.ext.svc.{MExtService, MExtServices}
import japgolly.univeq.UnivEq
import play.api.libs.json.Format

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.02.2020 22:50
  * Description: Модель осей для платформ.
  */
object MOsFamilies extends StringEnum[MOsFamily] {

  case object Android extends MOsFamily("Android") {
    override def appDistribServices: Seq[MExtService] =
      MExtServices.GooglePlay :: Nil
    override def cordovaPlatformId = Some( "android" )
    //override def appPackageFileExtensionsNoDot: Iterable[String] =
    //  "apk" :: Nil
  }

  /** Мобильная iOS для iphone/ipad/etc. */
  case object Apple_iOS extends MOsFamily( "Apple iOS") {
    override def appDistribServices =
      MExtServices.AppleITunes :: Nil
    //override def appPackageFileExtensionsNoDot =
    //  "ipa" :: Nil
    /** Файловые экстеншены (без точки). */
    override def cordovaPlatformId = Some( "ios" )
  }


  // ! При добавлении ОСей, надо править функцию-конвертер StatUtil.osFamilyConv


  override def values = findValues

}


sealed abstract class MOsFamily(override val value: String ) extends StringEnumEntry {

  /** Сервисы централизованной дистрибуции приложений под платформу. */
  def appDistribServices: Seq[MExtService] = Nil

  /** Файловые экстеншены (без точки). */
  //def appPackageFileExtensionsNoDot: Iterable[String] = Nil

  /** Название директории платформы в app/platforms/. */
  def cordovaPlatformId: Option[String]

}

object MOsFamily {

  @inline implicit def univEq: UnivEq[MOsFamily] = UnivEq.derive

  implicit def osPlatformJson: Format[MOsFamily] =
    EnumeratumUtil.valueEnumEntryFormat( MOsFamilies )


  implicit final class OsFamilyOpsExt( private val osf: MOsFamily ) extends AnyVal {

    /** Поддерживается ли минификация приложения силами cordova? */
    def isCdvAppMinimizable: Boolean = {
      osf match {
        case MOsFamilies.Android => true
        case _ => false
      }
    }

  }

}
