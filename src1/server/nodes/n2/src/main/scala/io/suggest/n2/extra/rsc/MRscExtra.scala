package io.suggest.n2.extra.rsc

import io.suggest.common.empty.EmptyUtil
import io.suggest.es.{IEsMappingProps, MappingDsl}
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.18 11:19
  * Description: Когда узел описывает удалённый ресурс (страницу) сюда запихиваются данные той страницы:
  * ссылка, хост и т.д.
  */
object MRscExtra
  extends IEsMappingProps
{

  object Fields {
    val URL_FN = "u"
    val HOST_NAMES_FN = "hn"
    val HOST_TOKENS_FN = "ht"
  }


  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    Json.obj(
      F.URL_FN -> FText.notIndexedJs,
      F.HOST_NAMES_FN -> FObject.nested(
        properties = MHostNameIndexed.esMappingProps,
      ),
      F.HOST_TOKENS_FN -> FKeyWord.indexedJs,
    )
  }

  /** Поддержка play-json. */
  implicit def mRscExtraJson: OFormat[MRscExtra] = {
    val F = Fields
    (
      (__ \ F.URL_FN).format[String] and
      (__ \ F.HOST_NAMES_FN).formatNullable[Seq[MHostNameIndexed]]
        .inmap[Seq[MHostNameIndexed]](
          EmptyUtil.opt2ImplEmpty1F(Nil),
          hosts => if (hosts.isEmpty) None else Some(hosts)
        ) and
      (__ \ F.HOST_TOKENS_FN).formatNullable[Set[String]]
        .inmap[Set[String]](
          EmptyUtil.opt2ImplEmptyF( Set.empty ),
          hToks => if (hToks.isEmpty) None else Some(hToks)
        )
    )(apply, unlift(unapply))
  }

}


/** Модель-контейнер данных http-ресурса.
  *
  * @param url Ссылка на описываемый ресурс.
  * @param hostNames Индексируемые имена хостов.
  * @param hostTokens Индексируемые токены хостнейма для сбора примерной статистики.
  */
case class MRscExtra(
                      url          : String,
                      hostNames    : Seq[MHostNameIndexed],
                      hostTokens   : Set[String]
                    )
