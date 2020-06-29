package io.suggest.sc.app

import io.suggest.dev.MOsFamily
import io.suggest.n2.edge.{MPredicate, MPredicates}
import japgolly.univeq._
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.12.2019 18:53
  * Description: Модель аргументов для реквеста на сервер для получения инфы
  * по скачиванию приложения (получения ссылки/метаданных на скачивание).
  */
object MScAppGetQs {

  object Fields {
    def OS_FAMILY = "o"
    def RDR = "r"
    def ON_NODE_ID = "n"
    def PREDICATE = "p"
  }

  /** Поддержка play-json. */
  implicit def scAppGetQsJson: OFormat[MScAppGetQs] = {
    val F = Fields
    (
      (__ \ F.OS_FAMILY).format[MOsFamily] and
      (__ \ F.RDR).format[Boolean] and
      (__ \ F.ON_NODE_ID).formatNullable[String] and
      {
        val fmt = (__ \ F.PREDICATE).formatNullable[MPredicate]
        val readsFiltered = fmt
          .filter { predOpt =>
            predOpt
              .fold(true) { _ eqOrHasParent MPredicates.Blob.File }
          }
        OFormat(readsFiltered, fmt)
      }
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MScAppGetQs] = UnivEq.derive


  def rdr = GenLens[MScAppGetQs]( _.rdr )
  def onNodeId = GenLens[MScAppGetQs]( _.onNodeId )
  def predicate = GenLens[MScAppGetQs]( _.predicate )

}


/** Контейнер данных запроса ссылки доступа к приложению.
  *
  * @param onNodeId id узла, на котором запрошено приложение.
  * @param rdr Вернуть редирект? Тогда нужно, чтобы был задан предикат и остальные координаты.
  * @param osFamily Платформа дистрибуции.
  *                 None - означает локальную раздачу файлов.
  * @param predicate Предикат, если требуется строго-конкретный результат.
  */
case class MScAppGetQs(
                        osFamily      : MOsFamily,
                        rdr           : Boolean,
                        onNodeId      : Option[String]         = None,
                        predicate     : Option[MPredicate]     = None,
                      )
