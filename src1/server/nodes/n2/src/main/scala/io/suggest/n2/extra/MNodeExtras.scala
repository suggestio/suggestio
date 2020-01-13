package io.suggest.n2.extra

import io.suggest.common.empty.{EmptyProduct, EmptyUtil, IEmpty}
import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.model.PrefixedFn
import io.suggest.n2.extra.doc.MNodeDoc
import io.suggest.n2.extra.domain.MDomainExtra
import io.suggest.n2.extra.rsc.MRscExtra
import io.suggest.vid.ext.MVideoExtInfo
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
  override val empty = MNodeExtras()


  /** Статическая модель полей модели [[MNodeExtras]]. */
  object Fields {

    /** ES-поля legacy-узлов AD Network. */
    object Adn extends PrefixedFn {
      val ADN_FN    = "a"
      override protected def _PARENT_FN = ADN_FN

      import MAdnExtra.{Fields => F}
      def IS_TEST_FN          = _fullFn( F.IS_TEST )
      def RIGHTS_FN           = _fullFn( F.RIGHTS )
      def SHOWN_TYPE_FN       = _fullFn( F.SHOWN_TYPE )
    }


    /** ES-поля iBeacon (BLE-маячка). */
    object Beacon extends PrefixedFn {
      val BEACON_FN   = "b"
      override protected def _PARENT_FN = BEACON_FN

      import MBeaconExtra.{Fields => F}
      def UUID_FN   = _fullFn( F.UUID_FN )
      def MAJOR_FN  = _fullFn( F.MAJOR_FN )
      def MINOR_FN  = _fullFn( F.MINOR_FN )
    }


    /** ES-поля данных по слинкованным интернет-доменам. */
    object Domain extends PrefixedFn {
      val DOMAIN_FN = "d"
      override protected def _PARENT_FN = DOMAIN_FN

      import MDomainExtra.{Fields => F}
      def DKEY_FN   = _fullFn( F.DKEY_FN )
      def MODE_FN   = _fullFn( F.MODE_FN )
    }


    object MNDoc extends PrefixedFn {
      val MNDOC_FN = "o"
      override protected def _PARENT_FN = MNDOC_FN

      // TODO Хз, надо ли это с момента создания. Document -- это шаблон без данных и не индексируем.
      import MNodeDoc.{Fields => F}
      def DOCUMENT_FN = _fullFn( F.TEMPLATE_FN )
    }


    object VideoExt extends PrefixedFn {
      val VIDEO_EXT_FN = "v"
      override protected def _PARENT_FN = VIDEO_EXT_FN

      import MVideoExtInfo.{Fields => F}
      def VIDEO_SERVICE_FN  = _fullFn( F.VIDEO_SERVICE_FN )
      def REMOTE_ID_FN      = _fullFn( F.REMOTE_ID_FN )
    }


    object Resource extends PrefixedFn {
      val RESOURCE_FN = "r"
      override protected def _PARENT_FN = RESOURCE_FN
    }

  }



  /** Поддержка JSON для растущей модели [[MNodeExtras]]. */
  implicit val nodeExtrasJson: OFormat[MNodeExtras] = (
    (__ \ Fields.Adn.ADN_FN).formatNullable[MAdnExtra] and
    (__ \ Fields.Beacon.BEACON_FN).formatNullable[MBeaconExtra] and
    (__ \ Fields.Domain.DOMAIN_FN).formatNullable[Seq[MDomainExtra]]
      .inmap [Seq[MDomainExtra]] (
        EmptyUtil.opt2ImplEmptyF(Nil),
        { domains => if (domains.isEmpty) None else Some(domains) }
      ) and
    (__ \ Fields.MNDoc.MNDOC_FN).formatNullable[MNodeDoc] and
    (__ \ Fields.VideoExt.VIDEO_EXT_FN).formatNullable[MVideoExtInfo] and
    (__ \ Fields.Resource.RESOURCE_FN).formatNullable[MRscExtra]
  )(apply, unlift(unapply))


  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    val objs1 = List[(String, IEsMappingProps)](
      F.Adn.ADN_FN            -> MAdnExtra,
      F.Beacon.BEACON_FN      -> MBeaconExtra,
      F.MNDoc.MNDOC_FN        -> MNodeDoc,
      F.VideoExt.VIDEO_EXT_FN -> MVideoExtInfo,
      F.Resource.RESOURCE_FN  -> MRscExtra,
    )
      .esSubModelsJsObjects( nested = false )

    objs1 ++ Json.obj(
      F.Domain.DOMAIN_FN -> FObject.nested( MDomainExtra.esMappingProps ),
    )
  }

  val adn       = GenLens[MNodeExtras](_.adn)
  val beacon    = GenLens[MNodeExtras](_.beacon)
  val domains   = GenLens[MNodeExtras](_.domains)
  val doc       = GenLens[MNodeExtras](_.doc)
  val extVideo  = GenLens[MNodeExtras](_.extVideo)
  val resource  = GenLens[MNodeExtras](_.resource)

}


/** Класс-контейнер-реализация модели.
  *
  * @param adn Данные для adn-узла (личного кабинета).
  * @param beacon Данные bluetooth-маячка.
  * @param domains Домены, подключенные к suggest.io в качестве интерфейса выдачи.
  * @param doc jd-документ.
  * @param extVideo Данные встраиваемого (связанного) видео, на каком-то видео-сервисе.
  * @param resource Интернет-ресурс, доступный по ссылке.
  */
final case class MNodeExtras(
                              adn       : Option[MAdnExtra]         = None,
                              beacon    : Option[MBeaconExtra]      = None,
                              domains   : Seq[MDomainExtra]         = Nil,
                              doc       : Option[MNodeDoc]          = None,
                              extVideo  : Option[MVideoExtInfo]     = None,
                              resource  : Option[MRscExtra]         = None,
                            )
  extends EmptyProduct
{

  def withDoc(doc: Option[MNodeDoc]) = copy(doc = doc)
  def withAdn(adn: Option[MAdnExtra]) = copy(adn = adn)

  def isRcvr = adn.exists(_.isReceiver)

  def adnLogo = adn.flatMap(_.resView.logo)
  def adnWcFg = adn.flatMap(_.resView.wcFg)
  def adnGalImgs = adn.toList.flatMap(_.resView.galImgs)
  def adnEdgeUidsIter = adn.iterator.flatMap(_.resView.edgeUids)

}
