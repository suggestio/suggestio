package models.mtag

import io.suggest.common.empty.EmptyProduct
import io.suggest.common.text.StringUtil
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.{Criteria, TagCriteria}
import io.suggest.model.n2.node.MNodeTypes
import io.suggest.model.n2.node.search.{MNodeSearch, MNodeSearchDfltImpl}
import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.sc.TagSearchConstants.Req._
import models.{GeoMode, GeoNone}
import play.api.mvc.QueryStringBindable

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.09.15 17:10
 * Description: Модель аргументов поискового запроса тегов, например в поисковой выдаче.
 * Поиск происходит по узлам графа N2, где теги -- лишь частный случай вершин.
 */
object MTagSearch {

  private def LIMIT_DFLT    = 10
  private def LIMIT_MAX     = 50
  private def OFFSET_MAX    = 200
  private def QUERY_MAXLEN  = 64

  /** Поддержка интеграции с play-роутером в области URL Query string. */
  implicit def qsb(implicit
                   strOptB    : QueryStringBindable[Option[String]],
                   intOptB    : QueryStringBindable[Option[Int]],
                   geoLocB    : QueryStringBindable[GeoMode]
                  ): QueryStringBindable[MTagSearch] = {
    new QueryStringBindableImpl[MTagSearch] {
      /** Биндинг значения [[MTagSearch]] из URL qs. */
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MTagSearch]] = {
        val k = key1F(key)
        for {
          eFtsQueryOpt        <- strOptB.bind (k(FACE_FTS_QUERY_FN),  params)
          eLimit              <- intOptB.bind (k(LIMIT_FN),           params)
          eOffset             <- intOptB.bind (k(OFFSET_FN),          params)
          eGeoLoc             <- geoLocB.bind (k(GEO_LOC_FN),         params)
        } yield {
          // TODO Нужно избегать пустого критерия поиска, т.е. возвращать None, когда нет параметров для поиска.
          for {
            _ftsQueryOpt      <- eFtsQueryOpt.right
            _limitOpt         <- eLimit.right
            _offsetOpt        <- eOffset.right
            _geoLoc           <- eGeoLoc.right
          } yield {

            val tags: Seq[String] = {
              val opt = for (_ftsQuery <- _ftsQueryOpt) yield {
                val q1 = StringUtil.trimLeft( _ftsQuery )
                val q2 = if (q1.length > QUERY_MAXLEN)
                  q1.substring(0, QUERY_MAXLEN)
                else
                  q1
                q2.split("[,;\\s#]")
                  .iterator
                  .filter { _.length <= QUERY_MAXLEN}
                  .toSeq
              }
              opt.getOrElse(Nil)
            }

            val limitOpt  = _limitOpt.filter(_ <= LIMIT_MAX)
            val offsetOpt = _offsetOpt.filter(_ <= OFFSET_MAX)

            MTagSearch(
              tags      = tags,
              limitOpt  = limitOpt,
              offsetOpt = offsetOpt,
              geoLoc    = _geoLoc
            )
          }
        }
      }

      /** Разбиндивание значения [[MTagSearch]] в URL qs. */
      override def unbind(key: String, value: MTagSearch): String = {
        _mergeUnbinded {
          val k = key1F(key)
          val tagsOpt = for (_ <- value.tags.headOption) yield {
            value.tags.mkString("#", ", #", "")
          }
          Iterator(
            strOptB.unbind(k(FACE_FTS_QUERY_FN),  tagsOpt),
            intOptB.unbind(k(LIMIT_FN),           Some(value.limit)),
            intOptB.unbind(k(OFFSET_FN),          Some(value.offset)),
            geoLocB.unbind(k(GEO_LOC_FN),         value.geoLoc)
          )
        }
      }

    }
  }

}


/** Дефолтовая реализация модели аргументов поиска тегов. */
case class MTagSearch(
  tags        : Seq[String] = Nil,
  limitOpt    : Option[Int] = None,
  offsetOpt   : Option[Int] = None,
  geoLoc      : GeoMode     = GeoNone
)
  extends EmptyProduct
{ that =>

  def limit = limitOpt.getOrElse( MTagSearch.LIMIT_DFLT )
  def offset = offsetOpt.getOrElse( 0 )

  def searchTagOpt = tags.lastOption

  def edgeSearchCriteria: Criteria = {
    val tcrOpt = for (q <- searchTagOpt) yield {
      TagCriteria(q, isPrefix = true)
    }
    Criteria(
      predicates  = Seq( MPredicates.TaggedBy.Self ),
      tags        = tcrOpt.toSeq
    )
  }



  def toEsSearch: MNodeSearch = {
    new MNodeSearchDfltImpl {
      override def outEdges  = Seq(that.edgeSearchCriteria)
      override def limit     = that.limit
      override def offset    = that.offset
      override def nodeTypes = Seq( MNodeTypes.Tag )
    }
  }

}
