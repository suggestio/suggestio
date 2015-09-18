package models.mtag

import io.suggest.model.n2.search.MNodeSearchDfltImpl
import play.api.mvc.QueryStringBindable
import util.qsb.QsbKey1T
import io.suggest.sc.TagSearchConstants.Req._
import views.js.tags.m.mtSearchJsUnbindTpl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.09.15 17:10
 * Description: Модель аргументов поискового запроса тегов, например в поисковой выдаче.
 * Поиск происходит по узлам графа N2, где теги -- лишь частный случай вершин.
 */
object MTagSearch {

  /** Поддержка интеграции с play-роутером в области URL Query string. */
  implicit def qsb(implicit
                   strOptB    : QueryStringBindable[Option[String]],
                   intB       : QueryStringBindable[Int]
                  ): QueryStringBindable[MTagSearch] = {
    new QueryStringBindable[MTagSearch] with QsbKey1T {
      /** Биндинг значения [[MTagSearch]] из URL qs. */
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MTagSearch]] = {
        val k = key1F(key)
        for {
          eFtsQueryOpt        <- strOptB.bind (k(FACE_FTS_QUERY_FN),  params)
          eLimit              <- intB.bind    (k(LIMIT_FN),           params)
          eOffset             <- intB.bind    (k(OFFSET_FN),          params)
        } yield {
          // TODO Нужно избегать пустого критерия поиска, т.е. возвращать None, когда нет параметров для поиска.
          for {
            _ftsQueryOpt      <- eFtsQueryOpt.right
            _limit            <- eLimit.right
            _offset           <- eOffset.right
          } yield {
            new MTagSearch {
              override def tagVxFace = _ftsQueryOpt
              override def limit     = _limit
              override def offset    = _offset
            }
          }
        }
      }

      /** Разбиндивание значения [[MTagSearch]] в URL qs. */
      override def unbind(key: String, value: MTagSearch): String = {
        val k = key1F(key)
        Iterator(
          strOptB.unbind  (k(FACE_FTS_QUERY_FN),  value.tagVxFace),
          intB.unbind     (k(LIMIT_FN),           value.limit),
          intB.unbind     (k(OFFSET_FN),          value.offset)
        )
          .filter { !_.isEmpty }
          .mkString("&")
      }

      override def javascriptUnbind: String = {
        mtSearchJsUnbindTpl(KEY_DELIM).body
      }
    }
  }

}


/** Дефолтовая реализация модели аргументов поиска тегов. */
class MTagSearch
  extends MNodeSearchDfltImpl
