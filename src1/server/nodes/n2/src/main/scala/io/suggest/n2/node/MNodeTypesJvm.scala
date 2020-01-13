package io.suggest.n2.node

import io.suggest.common.empty.{EmptyUtil, OptionUtil}
import io.suggest.common.html.HtmlConstants
import io.suggest.enum2.EnumeratumJvmUtil
import io.suggest.n2.edge.MPredicates
import io.suggest.xplay.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable
import japgolly.univeq._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.09.15 11:51
 * Description: Enum-модель типов узлов N2: карточка, adn-узел, тег, картинка, видео, и т.д.
 */

object MNodeTypesJvm {

  /** Подборка отображаемого названия узла на основе его типа. */
  def guessNodeDisplayName(mnode: MNode): Option[String] = {
    guessNodeDisplayName(mnode.common.ntype, mnode)
  }
  /** Подборка отображаемого названия узла на основе указанного типа узла. */
  def guessNodeDisplayName(ntype: MNodeType, mnode: MNode): Option[String] = {
    ntype match {

      // Для юзера: можно поковыряться в email'ах.
      case MNodeTypes.Person =>
        mnode.meta.person
          .emails.headOption
          .orElse {
            import mnode.meta.person._
            OptionUtil.maybe( nameFirst.nonEmpty || nameLast.nonEmpty ) {
              nameFirst.fold("")(_ + HtmlConstants.SPACE) + nameLast.getOrElse("")
            }
          }
          // Переезд идентов внутрь узлов позволяет использовать иденты вместо имени.
          .orElse {
            val I = MPredicates.Ident
            val iter0 = mnode.edges
              .withPredicateIter( I.Email, I.Phone, I.Id )
              .flatMap { medge =>
                for {
                  key <- medge.nodeIds
                  if key.nonEmpty
                } yield {
                  (key, medge.predicate, medge.info.extService)
                }
              }
            OptionUtil.maybe( iter0.nonEmpty ) {
              val (key, pred, extServiceOpt) = iter0.minBy {
                case (_, pred, _) =>
                  pred match {
                    case I.Email => 1
                    case I.Phone => 2
                    case I.Id    => 3
                    case _       => 4
                  }
              }
              extServiceOpt
                .filter(_ => pred !=* I.Id)
                .fold(key) { extSvc =>
                  extSvc.value + HtmlConstants.MINUS + key
                }
            }
          }

      // Для тега: ковыряемся в эджах узла на предмет тегов.
      case MNodeTypes.Tag =>
        mnode.edges
          .out
          .iterator
          .flatMap(_.info.tags)
          .buffered
          .headOption

      // Для остальных типов узлов это всё неактуально.
      case _ =>
        None
    }
  }

  implicit def nodeTypeQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[MNodeType] =
    EnumeratumJvmUtil.valueEnumQsb( MNodeTypes )

  import play.api.data._, Forms._

  /** Опциональный маппинг для play-формы. */
  def mappingOptM: Mapping[Option[MNodeType]] = {
    optional( nonEmptyText(minLength = 1, maxLength = 10) )
      .transform [Option[MNodeType]] (
        _.flatMap( MNodeTypes.withValueOpt ),
        _.map(_.value)
      )
  }

  /** Обязательный маппинг для play-формы. */
  def mappingM: Mapping[MNodeType] = {
    mappingOptM
      .verifying("error.required", _.isDefined)
      .transform [MNodeType] (EmptyUtil.getF, EmptyUtil.someF)
  }

}
