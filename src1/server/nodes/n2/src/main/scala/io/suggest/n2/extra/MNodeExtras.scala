package io.suggest.n2.extra

import io.suggest.common.empty.{EmptyProduct, EmptyUtil, IEmpty}
import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.model.PrefixedFn
import io.suggest.n2.extra.doc.MNodeDoc
import io.suggest.n2.extra.domain.MDomainExtra
import io.suggest.n2.extra.rsc.MRscExtra
import io.suggest.vid.ext.MVideoExtInfo
import io.suggest.xplay.json.PlayJsonUtil
import monocle.macros.GenLens
import play.api.libs.functional.syntax._
import play.api.libs.json.{OFormat, _}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 14.09.15 10:25
 * Description: Модель-контейнер для различных субмоделей (ADN node, ad, tag, place, etc) одного узла N2.
 * В рамках extra-моделей узел N2 как бы "специализируется" на своей задаче.
 */
object MNodeExtras
  extends IEsMappingProps
  with IEmpty
{

  override type T = MNodeExtras

  /** Расшаренный пустой экземпляр для дедубликации пустых инстансов контейнера в памяти. */
  override val empty = apply()


  /** Статическая модель полей модели [[MNodeExtras]]. */
  object Fields {

    /** ES-поля legacy-узлов AD Network. */
    object Adn extends PrefixedFn {
      final val ADN_FN    = "adnNode"
      override protected def _PARENT_FN = ADN_FN

      import MAdnExtra.{Fields => F}
      final def IS_TEST_FN          = _fullFn( F.IS_TEST )
      final def RIGHTS_FN           = _fullFn( F.RIGHTS )
      final def SHOWN_TYPE_FN       = _fullFn( F.SHOWN_TYPE )
    }


    /** ES-поля данных по слинкованным интернет-доменам. */
    object Domain extends PrefixedFn {
      final val DOMAIN_FN = "domain"
      override protected def _PARENT_FN = DOMAIN_FN

      import MDomainExtra.{Fields => F}
      final def DKEY_FN   = _fullFn( F.DOMAIN_KEY_FN )
      final def MODE_FN   = _fullFn( F.MODE_FN )
    }


    object Doc extends PrefixedFn {
      final val DOCUMENT_FN = "document"
      override protected def _PARENT_FN = DOCUMENT_FN
    }


    object VideoExt extends PrefixedFn {
      final val VIDEO_EXT_FN = "videoExt"
      override protected def _PARENT_FN = VIDEO_EXT_FN

      import MVideoExtInfo.{Fields => F}
      final def VIDEO_SERVICE_FN  = _fullFn( F.VIDEO_SERVICE_FN )
      final def REMOTE_ID_FN      = _fullFn( F.REMOTE_ID_FN )
    }


    object Resource extends PrefixedFn {
      final val RESOURCE_FN = "resource"
      override protected def _PARENT_FN = RESOURCE_FN
    }


    object Calendar extends PrefixedFn {
      final val CALENDAR_FN = "calendar"
      override protected def _PARENT_FN = CALENDAR_FN
    }

    object CryptoKey extends PrefixedFn {
      final val CRYPTO_KEY_FN = "cryptoKey"
      override protected def _PARENT_FN = CRYPTO_KEY_FN
    }

  }


  /** Поддержка JSON для растущей модели [[MNodeExtras]]. */
  implicit val nodeExtrasJson: OFormat[MNodeExtras] = (
    PlayJsonUtil.fallbackPathFormatNullable[MAdnExtra]( Fields.Adn.ADN_FN, "a" ) and
    PlayJsonUtil.fallbackPathFormatNullable[Seq[MDomainExtra]]( Fields.Domain.DOMAIN_FN, "d" )
      .inmap [Seq[MDomainExtra]] (
        EmptyUtil.opt2ImplEmptyF(Nil),
        { domains => if (domains.isEmpty) None else Some(domains) }
      ) and
    PlayJsonUtil.fallbackPathFormatNullable[MNodeDoc]( Fields.Doc.DOCUMENT_FN, "o" ) and
    PlayJsonUtil.fallbackPathFormatNullable[MVideoExtInfo]( Fields.VideoExt.VIDEO_EXT_FN, "v" ) and
    PlayJsonUtil.fallbackPathFormatNullable[MRscExtra]( Fields.Resource.RESOURCE_FN, "r" ) and
    (__ \ Fields.Calendar.CALENDAR_FN).formatNullable[MNodeCalendar] and
    (__ \ Fields.CryptoKey.CRYPTO_KEY_FN).formatNullable[MNodeCryptoKey]
  )(apply, unlift(unapply))


  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    val objs1 = List[(String, IEsMappingProps)](
      F.Adn.ADN_FN            -> MAdnExtra,
      F.Doc.DOCUMENT_FN        -> MNodeDoc,
      F.VideoExt.VIDEO_EXT_FN -> MVideoExtInfo,
      F.Resource.RESOURCE_FN  -> MRscExtra,
      F.Calendar.CALENDAR_FN  -> MNodeCalendar,
      F.CryptoKey.CRYPTO_KEY_FN -> MNodeCryptoKey,
    )
      .esSubModelsJsObjects( nested = false )

    objs1 ++ Json.obj(
      F.Domain.DOMAIN_FN -> FObject.nested( MDomainExtra.esMappingProps ),
    )
  }

  def adn       = GenLens[MNodeExtras](_.adn)
  def domains   = GenLens[MNodeExtras](_.domains)
  def doc       = GenLens[MNodeExtras](_.doc)
  def extVideo  = GenLens[MNodeExtras](_.extVideo)
  def resource  = GenLens[MNodeExtras](_.resource)
  def calendar  = GenLens[MNodeExtras](_.calendar)
  def cryptoKey = GenLens[MNodeExtras](_.cryptoKey)


  implicit final class NodeExtrasOpsExt( private val nodeExt: MNodeExtras ) extends AnyVal {

    def isRcvr = nodeExt.adn.exists(_.isReceiver)

    def adnLogo = nodeExt.adn.flatMap(_.resView.logo)
    def adnWcFg = nodeExt.adn.flatMap(_.resView.wcFg)
    def adnGalImgs = nodeExt.adn.toList.flatMap(_.resView.galImgs)
    def adnEdgeUidsIter = nodeExt.adn.iterator.flatMap(_.resView.edgeUids)

  }

}


/** Класс-контейнер-реализация модели.
  *
  * @param adn Данные для adn-узла (личного кабинета).
  * @param beacon Данные bluetooth-маячка.
  * @param domains Домены, подключенные к suggest.io в качестве интерфейса выдачи.
  * @param doc jd-документ.
  * @param extVideo Данные встраиваемого (связанного) видео, на каком-то видео-сервисе.
  * @param resource Интернет-ресурс, доступный по ссылке.
  * @param calendar Calendar data.
  * @param cryptoKey Cryptographic key storage.
  */
final case class MNodeExtras(
                              adn       : Option[MAdnExtra]         = None,
                              domains   : Seq[MDomainExtra]         = Nil,
                              doc       : Option[MNodeDoc]          = None,
                              extVideo  : Option[MVideoExtInfo]     = None,
                              resource  : Option[MRscExtra]         = None,
                              calendar  : Option[MNodeCalendar]     = None,
                              cryptoKey : Option[MNodeCryptoKey]    = None,
                            )
  extends EmptyProduct
