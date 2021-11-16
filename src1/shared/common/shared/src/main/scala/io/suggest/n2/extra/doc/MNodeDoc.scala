package io.suggest.n2.extra.doc

import io.suggest.es.{EsConstants, IEsMappingProps, MappingDsl}
import io.suggest.jd.tags.JdTag
import play.api.libs.functional.syntax._
import play.api.libs.json._
import io.suggest.scalaz.ZTreeUtil.{ZTREE_FORMAT, zTreeUnivEq}
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 15:55
  * Description: Кросс-платформенная модель данных по рекламной карточки второй поколения.
  *
  * Эта модель подразумевает использование JSON-документа и эджи вместо списка текстовых полей
  * с текстом, графикой и всем остальным.
  *
  * Явно непустая-модель, подразумевается использование Option[MNodeDoc] для пустой модели.
  *
  * Модель используется в первую очередь на сервере внутри mnode.extras.ad.
  */
object MNodeDoc
  extends IEsMappingProps
{

  @inline implicit def univEq: UnivEq[MNodeDoc] = UnivEq.derive

  /** Модель названий полей модели [[MNodeDoc]]. */
  object Fields {

    final def TEMPLATE_FN = "t"
    final def HTML_FN = "html"

  }


  /** Поддержка play-json. */
  implicit val nodeDocJson: OFormat[MNodeDoc] = {
    (
      (__ \ Fields.TEMPLATE_FN).format[Tree[JdTag]] and
      (__ \ Fields.HTML_FN).formatNullable[String]
    )(apply, unlift(unapply))
  }

  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    Json.obj(
      F.TEMPLATE_FN -> FObject.disabled,
      F.HTML_FN -> FText(
        analyzer = Some( EsConstants.CONTENT_FTS_INDEX_ANALYZER ),
      ),
    )
  }

  def template = GenLens[MNodeDoc](_.template)
  def html = GenLens[MNodeDoc](_.html)

}


/** Класс модели данных по узлу, который является рекламной карточкой.
  *
  * @param template JSON-Document, т.е. шаблон, описывающий рендер какого-то внешнего контента.
  *                 Сам контент является внешним по отношению к шаблону.
  *                 В узле контент представлен эджами, которые слинкованы с документом по предикатам и/или edge-uid'ам.
  * @param html HTML-version of document.
  *             For ad-editor, rendered on editor client-side and POSTed from client.
  */
case class MNodeDoc(
                     template   : Tree[JdTag],
                     html       : Option[String] = None,
                   )
