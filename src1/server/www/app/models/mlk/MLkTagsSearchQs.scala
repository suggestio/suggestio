package models.mlk

import io.suggest.common.empty.EmptyProduct
import io.suggest.model.play.qsb.{QsbUtil, QueryStringBindableImpl}
import io.suggest.sc.TagSearchConstants.Req._
import play.api.mvc.QueryStringBindable

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.09.15 17:10
 * Description: Модель аргументов поискового запроса тегов, например в поисковой выдаче.
 * Поиск происходит по узлам графа N2, где теги -- лишь частный случай вершин.
 */
object MLkTagsSearchQs {

  /** Макс.длина текстового поискового запроса. */
  private def TAGS_QUERY_MAXLEN   = 64

  /** Максимальное значение limit в qs. */
  private def LIMIT_MAX           = 50

  /** Максимальное значение offset в qs. */
  private def OFFSET_MAX          = 200


  /** Поддержка интеграции с play-роутером в области URL Query string. */
  implicit def mLkTagsSearchQsQsb(implicit
                                  strB       : QueryStringBindable[String],
                                  intOptB    : QueryStringBindable[Option[Int]]
                                 ): QueryStringBindable[MLkTagsSearchQs] = {

    new QueryStringBindableImpl[MLkTagsSearchQs] {

      /** Биндинг значения [[MLkTagsSearchQs]] из URL qs. */
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MLkTagsSearchQs]] = {
        val k = key1F(key)
        for {
          tagsQueryE           <- strB.bind    (k(FACE_FTS_QUERY_FN),  params)
          limitOptE            <- intOptB.bind (k(LIMIT_FN),           params)
          offsetOptE           <- intOptB.bind (k(OFFSET_FN),          params)
        } yield {
          // TODO Нужно избегать пустого критерия поиска, т.е. возвращать None, когда нет параметров для поиска.
          for {
            _tagsQuery      <- QsbUtil.ensureStrLen(tagsQueryE, 1, TAGS_QUERY_MAXLEN)
              .right
            _limitOpt       <- QsbUtil.ensureIntOptRange(limitOptE, 1, LIMIT_MAX)
              .right
            _offsetOpt      <- QsbUtil.ensureIntOptRange(offsetOptE, 0, OFFSET_MAX)
              .right
          } yield {
            MLkTagsSearchQs(
              tagsQuery = _tagsQuery,
              limitOpt  = _limitOpt,
              offsetOpt = _offsetOpt
            )
          }
        }
      }

      /** Разбиндивание значения [[MLkTagsSearchQs]] в URL qs. */
      override def unbind(key: String, value: MLkTagsSearchQs): String = {
        _mergeUnbinded {
          val k = key1F(key)
          Iterator(
            strB.unbind     (k(FACE_FTS_QUERY_FN),  value.tagsQuery ),
            intOptB.unbind  (k(LIMIT_FN),           value.limitOpt  ),
            intOptB.unbind  (k(OFFSET_FN),          value.offsetOpt )
          )
        }
      }
    }
  }

}


/** Дефолтовая реализация модели аргументов поиска тегов. */
case class MLkTagsSearchQs(
  tagsQuery   : String,
  limitOpt    : Option[Int] = None,
  offsetOpt   : Option[Int] = None
)
  extends EmptyProduct
