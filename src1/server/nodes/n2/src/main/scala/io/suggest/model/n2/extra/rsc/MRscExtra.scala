package io.suggest.model.n2.extra.rsc

import io.suggest.common.empty.EmptyUtil
import io.suggest.es.model.IGenEsMappingProps
import io.suggest.es.util.SioEsUtil._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.18 11:19
  * Description: Когда узел описывает удалённый ресурс (страницу) сюда запихиваются данные той страницы:
  * ссылка, хост и т.д.
  */
object MRscExtra extends IGenEsMappingProps {

  object Fields {
    val URL_FN = "u"
    val HOST_NAMES_FN = "hn"
    val HOST_TOKENS_FN = "ht"
  }


  /** Список ES-полей модели. */
  override def generateMappingProps: List[DocField] = {
    List(
      FieldText( Fields.URL_FN, index = false, include_in_all = false ),
      FieldNestedObject( Fields.HOST_NAMES_FN, enabled = true, properties = MHostNameIndexed.generateMappingProps ),
      FieldKeyword( Fields.HOST_TOKENS_FN, index = true, include_in_all = false )
    )
  }


  /** Поддержка play-json. */
  implicit def mRscExtraFormat: OFormat[MRscExtra] = (
    (__ \ Fields.URL_FN).format[String] and
    (__ \ Fields.HOST_NAMES_FN).formatNullable[Seq[MHostNameIndexed]]
      .inmap[Seq[MHostNameIndexed]](
        EmptyUtil.opt2ImplEmpty1F(Nil),
        hosts => if (hosts.isEmpty) None else Some(hosts)
      ) and
    (__ \ Fields.HOST_TOKENS_FN).formatNullable[Set[String]]
      .inmap[Set[String]](
        EmptyUtil.opt2ImplEmptyF( Set.empty ),
        hToks => if (hToks.isEmpty) None else Some(hToks)
      )
  )(apply, unlift(unapply))

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
