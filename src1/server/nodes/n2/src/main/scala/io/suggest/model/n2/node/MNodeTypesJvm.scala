package io.suggest.model.n2.node

import io.suggest.common.empty.EmptyUtil
import io.suggest.model.play.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable

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
            if (nameFirst.nonEmpty || nameLast.nonEmpty) {
              val nameFull = nameFirst.fold("")(_ + " ") + nameLast.getOrElse("")
              Some(nameFull)
            } else {
              None
            }
          }

      // Для тега: ковыряемся в эджах узла на предмет тегов.
      case MNodeTypes.Tag =>
        mnode.edges
          .iterator
          .flatMap(_.info.tags)
          .toStream
          .headOption

      // Для остальных типов узлов это всё неактуально.
      case _ =>
        None
    }
  }


  /** Поддержка binding'а из URL query string, для play router'а. */
  implicit def mNodeTypeQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[MNodeType] = {
    new QueryStringBindableImpl[MNodeType] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MNodeType]] = {
        for (strIdEith <- strB.bind(key, params)) yield {
          strIdEith.right.flatMap { strId =>
            MNodeTypes.withValueOpt(strId)
              .fold [Either[String, MNodeType]] {
                Left("node.type.invalid")
              } { ntype =>
                Right(ntype)
              }
          }
        }
      }
      override def unbind(key: String, value: MNodeType): String = {
        strB.unbind(key, value.value)
      }
    }
  }


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
