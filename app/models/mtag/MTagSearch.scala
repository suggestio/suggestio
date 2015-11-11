package models.mtag

import io.suggest.common.text.StringUtil
import io.suggest.model.n2.node.{MNodeTypes, MNodeType}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
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

  def LIMIT_DFLT    = 10
  def LIMIT_MAX     = 50
  def OFFSET_MAX    = 500
  def QUERY_MAXLEN  = 64

  /** Поддержка интеграции с play-роутером в области URL Query string. */
  implicit def qsb(implicit
                   strOptB    : QueryStringBindable[Option[String]],
                   intOptB    : QueryStringBindable[Option[Int]]
                  ): QueryStringBindable[MTagSearch] = {
    new QueryStringBindable[MTagSearch] with QsbKey1T {
      /** Биндинг значения [[MTagSearch]] из URL qs. */
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MTagSearch]] = {
        val k = key1F(key)
        for {
          eFtsQueryOpt        <- strOptB.bind (k(FACE_FTS_QUERY_FN),  params)
          eLimit              <- intOptB.bind (k(LIMIT_FN),           params)
          eOffset             <- intOptB.bind (k(OFFSET_FN),          params)
        } yield {
          // TODO Нужно избегать пустого критерия поиска, т.е. возвращать None, когда нет параметров для поиска.
          for {
            _ftsQueryOpt      <- eFtsQueryOpt.right
            _limitOpt         <- eLimit.right
            _offsetOpt        <- eOffset.right
          } yield {
            val _ftsQueryOpt2 = _ftsQueryOpt.map { q =>
              val q1 = StringUtil.trimLeft(q)
              if (q1.length > QUERY_MAXLEN)
                q1.substring(0, QUERY_MAXLEN)
              else
                q1
            }
            val _limit = _limitOpt
              .fold(LIMIT_DFLT) { l =>
                Math.max(0,
                  Math.min(LIMIT_MAX, l))
              }
            val _offset = _offsetOpt
              .fold(0) { o =>
                Math.max(0,
                  Math.min(OFFSET_MAX, o))
              }
            new MTagSearch {
              override def tagVxFace = _ftsQueryOpt2
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
          intOptB.unbind  (k(LIMIT_FN),           Some(value.limit)),
          intOptB.unbind  (k(OFFSET_FN),          Some(value.offset))
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
class MTagSearch extends MNodeSearchDfltImpl {

  override def limit = MTagSearch.LIMIT_DFLT

  override def nodeTypes: Seq[MNodeType] = {
    Seq( MNodeTypes.Tag )
  }

}
