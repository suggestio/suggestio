package io.suggest.sc.tags

import io.suggest.geo.MLocEnv
import io.suggest.model.play.qsb.{QsbUtil, QueryStringBindableImpl}
import io.suggest.sc.MScApiVsn
import io.suggest.sc.ScConstants.ReqArgs.VSN_FN
import io.suggest.sc.TagSearchConstants.Req._
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.12.17 11:09
  * Description: JVM-only утиль для модели MScTagsSearchQs.
  */
object MScTagsSearchQsJvm {

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
                                  locEnvB    : QueryStringBindable[MLocEnv],
                                  apiVsnB    : QueryStringBindable[MScApiVsn]
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
          rcvrIdOptE          <- strOptB.bind (k(RCVR_ID_FN),         params)
          apiVsnE             <- apiVsnB.bind (k(VSN_FN),             params)
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
            _rcvrIdOpt        <- QsbUtil.ensureStrOptLen(rcvrIdOptE, 10, 100)
            _apiVsn           <- apiVsnE.right
          } yield {
            MScTagsSearchQs(
              tagsQuery = _tagsQueryOpt,
              limitOpt  = _limitOpt,
              offsetOpt = _offsetOpt,
              locEnv    = _locEnv,
              rcvrId    = _rcvrIdOpt,
              apiVsn    = _apiVsn
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
          locEnvB.unbind( k(LOC_ENV_FN),         value.locEnv     ),
          strOptB.unbind( k(RCVR_ID_FN),         value.rcvrId     ),
          apiVsnB.unbind( k(VSN_FN),             value.apiVsn     )
        )
      }

    }
  }

}
