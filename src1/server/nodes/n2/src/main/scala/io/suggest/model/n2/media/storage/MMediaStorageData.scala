package io.suggest.model.n2.media.storage

import io.suggest.xplay.qsb.QueryStringBindableImpl
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.12.2019 22:41
  * Description: Инфа по хранилищу. НЕ сохраняется в БД, живёт только в рамках запросов.
  * На сервере - данные из этой модели как-то распихивается внутри эджа.
  */
object MMediaStorageData {

  object Fields {
    def STORAGE_FN = "s"
    def INFO_FN = "i"
  }

  // Поддержка JSON. Вероятно, это не нужно.
  implicit def mediaStorageDataJson: OFormat[MMediaStorageData] = {
    val F = Fields
    (
      (__ \ F.STORAGE_FN).format[MStorage] and
      (__ \ F.INFO_FN).format[String]
    )(apply, unlift(unapply))
  }


  @inline implicit def univEq: UnivEq[MMediaStorageData] = UnivEq.derive

  implicit def mediaStorageDataQsb(implicit
                                   storageB: QueryStringBindable[MStorage],
                                   strB: QueryStringBindable[String],
                                  ): QueryStringBindable[MMediaStorageData] = {
    new QueryStringBindableImpl[MMediaStorageData] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MMediaStorageData]] = {
        val k = key1F(key)
        for {
          storageE <- storageB.bind( k(Fields.STORAGE_FN), params )
          infoE    <- strB.bind( k(Fields.INFO_FN), params )
        } yield {
          for {
            storage <- storageE
            info    <- infoE
          } yield {
            MMediaStorageData(
              storage = storage,
              info    = info,
            )
          }
        }
      }

      override def unbind(key: String, value: MMediaStorageData): String = {
        val k = key1F(key)
        _mergeUnbinded1(
          storageB.unbind( k(Fields.STORAGE_FN), value.storage ),
          strB.unbind( k(Fields.INFO_FN), value.info ),
        )
      }
    }
  }

}


/** Данные любого хранилища.
  *
  * @param storage Тип хранилища.
  * @param info Строка инфы по файлу в рамках указанного хранилища.
  *             Для swfs - это fid.
  *             Для ассета - это неизменяемая часть названия.
  *             Для хост-файла - это путь до файла, опционально с указанием хоста: /path/to/file, ./file, s2:/path/to
  */
case class MMediaStorageData(
                              storage   : MStorage,
                              info      : String,
                            )
