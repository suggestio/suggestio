package models.msc.tag

import io.suggest.common.empty.EmptyProduct
import io.suggest.geo.MLocEnv
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
object MScTagsSearchQs {

 /** Максимальное значение limit в qs. */
  private def LIMIT_MAX           = 50

  /** Максимальное значение offset в qs. */
  private def OFFSET_MAX          = 200

  /** Максимальная символьная длина текстового запроса тегов. */
  private def TAGS_QUERY_LEN_MAX  = 64


  /** Поддержка интеграции с play-роутером в области URL Query string. */
  implicit def mScTagsSearchQsQsb(implicit
                                  strOptB    : QueryStringBindable[Option[String]],
                                  intOptB    : QueryStringBindable[Option[Int]],
                                  locEnvB    : QueryStringBindable[MLocEnv]
                                 ): QueryStringBindable[MScTagsSearchQs] = {

    new QueryStringBindableImpl[MScTagsSearchQs] {

      /** Биндинг значения [[MScTagsSearchQs]] из URL qs. */
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MScTagsSearchQs]] = {
        val k = key1F(key)
        for {
          tagsQueryOptE       <- strOptB.bind (k(FACE_FTS_QUERY_FN),  params)
          limitOptE           <- intOptB.bind (k(LIMIT_FN),           params)
          offsetOptE          <- intOptB.bind (k(OFFSET_FN),          params)
          locEnvE             <- locEnvB.bind (k(LOC_ENV_FN),         params)
        } yield {
          // TODO Нужно избегать пустого критерия поиска, т.е. возвращать None, когда нет параметров для поиска.
          for {
            _tagsQueryOpt     <- QsbUtil.ensureStrOptLen(tagsQueryOptE, 0, TAGS_QUERY_LEN_MAX)
              .right
            _limitOpt         <- QsbUtil.ensureIntOptRange(limitOptE,  1, LIMIT_MAX)
              .right
            _offsetOpt        <- QsbUtil.ensureIntOptRange(offsetOptE, 0, OFFSET_MAX)
              .right
            _locEnv           <- locEnvE.right
          } yield {
            MScTagsSearchQs(
              tagsQuery = _tagsQueryOpt,
              limitOpt  = _limitOpt,
              offsetOpt = _offsetOpt,
              locEnv    = _locEnv
            )
          }
        }
      }

      /** Разбиндивание значения [[MScTagsSearchQs]] в URL qs. */
      override def unbind(key: String, value: MScTagsSearchQs): String = {
        val k = key1F(key)
        _mergeUnbinded1(
          strOptB.unbind( k(FACE_FTS_QUERY_FN),  value.tagsQuery  ),
          intOptB.unbind( k(LIMIT_FN),           value.limitOpt   ),
          intOptB.unbind( k(OFFSET_FN),          value.offsetOpt  ),
          locEnvB.unbind( k(LOC_ENV_FN),         value.locEnv     )
        )
      }

    }
  }

}


/** Дефолтовая реализация модели аргументов поиска тегов. */
case class MScTagsSearchQs(
  tagsQuery   : Option[String]  = None,
  limitOpt    : Option[Int]     = None,
  offsetOpt   : Option[Int]     = None,
  locEnv      : MLocEnv         = MLocEnv.empty
)
  extends EmptyProduct
