package io.suggest.n2.node

import io.suggest.geo.MNodeGeoLevel
import io.suggest.model._
import io.suggest.n2.bill.MNodeBilling
import io.suggest.n2.bill.tariff.MNodeTariffs
import io.suggest.n2.edge.MNodeEdges
import io.suggest.n2.node.common.MNodeCommon
import io.suggest.n2.extra.MNodeExtras
import io.suggest.n2.node.meta.MMeta

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 13.10.15 15:55
 * Description: Названия полей модели [[MNode]] вынесены в отдельный статический класс.
 * Это нужно для упрощения реализации DI-компаньона для модели [[MNode]]:
 * поисковые трейты стали независимыми от объекта-компаньона [[MNode]].
 */

object MNodeFields {

  /** Абсолютные имена ES-полей в .common */
  object Common extends PrefixedFn {
    def COMMON_FN = "common"
    override protected def _PARENT_FN = COMMON_FN

    import MNodeCommon.{Fields => F}
    def NODE_TYPE_FN      = _fullFn( F.NODE_TYPE_FN )
    def IS_ENABLED_FN     = _fullFn( F.IS_ENABLED_FN )
    def IS_DEPENDENT_FN   = _fullFn( F.IS_DEPEND_FN )
  }

  /** Абсолютные имена ES-полей в .meta */
  object Meta extends PrefixedFn {
    /** Имя поля на стороне ES, куда скидываются все метаданные. */
    def META_FN                     = "meta"
    override protected def _PARENT_FN = META_FN

    import MMeta.{Fields => F}
    def META_BASIC_FN               = _fullFn( F.Basic.BASIC_FN )
    def BASIC_NAME_SHORT_NOTOK_FN   = _fullFn( F.Basic.NAME_SHORT_NOTOK_FN )
    def DATE_CREATED_FN             = _fullFn( F.Basic.DATE_CREATED_FN )
    def META_COLORS_FN              = _fullFn( F.Colors.COLORS_FN )
    def META_COLORS_BG_CODE_FN      = _fullFn( F.Colors.BG_CODE_FN )
    def META_COLORS_FG_CODE_FN      = _fullFn( F.Colors.FG_CODE_FN )
    def META_BASIC_NAME_FN          = _fullFn( F.Basic.BASIC_NAME_FN )
  }

  /** Абсолютные имена ES-полей в .extras */
  object Extras extends PrefixedFn {

    def EXTRAS_FN               = "extras"
    override protected def _PARENT_FN = EXTRAS_FN

    import MNodeExtras.{Fields => F}

    def ADN_IS_TEST_FN          = _fullFn( F.Adn.IS_TEST_FN )
    def ADN_RIGHTS_FN           = _fullFn( F.Adn.RIGHTS_FN )
    def ADN_SHOWN_TYPE_FN       = _fullFn( F.Adn.SHOWN_TYPE_FN )

    def DOMAIN_FN               = _fullFn( F.Domain.DOMAIN_FN )
    def DOMAIN_DKEY_FN          = _fullFn( F.Domain.DKEY_FN )
    def DOMAIN_MODE_FN          = _fullFn( F.Domain.MODE_FN )

  }

  /** Абсолютные имена ES-полей в .edges */
  object Edges extends PrefixedFn {

    def EDGES_FN = "edges"
    override protected def _PARENT_FN = EDGES_FN

    /** Адрес nested-объектов, хранящих данные по эджам. */
    val E_OUT_FN = _fullFn( MNodeEdges.Fields.OUT_FN )

    import MNodeEdges.Fields.Out

    // Корневые поля MEdge. Некоторые поля используются очень часто, поэтому они за'val'ены.
    val EO_PREDICATE_FN = _fullFn( Out.O_PREDICATE_FN )
    val EO_NODE_IDS_FN = _fullFn( Out.O_NODE_ID_FN )
    def EO_ORDER_FN = _fullFn( Out.O_ORDER_FN )
    def EO_INFO_FLAG_FN = _fullFn( Out.O_INFO_FLAG_FN )
    def EO_INFO_FLAGS_FN = _fullFn( Out.O_INFO_FLAGS_FN )
    def EO_INFO_FLAGS_FLAG_FN = _fullFn( Out.O_INFO_FLAGS_FLAG_FN )
    def EO_INFO_DATE_FN = _fullFn( Out.O_INFO_DATE_FN )

    // Теги
    def EO_INFO_TAGS_FN = _fullFn( Out.O_INFO_TAGS_FN )
    def EO_INFO_TAGS_RAW_FN = _fullFn( Out.O_INFO_TAGS_RAW_FN )

    // Гео-шейпы
    def EO_INFO_GS_FN = _fullFn( Out.O_INFO_GS_FN )
    def EO_INFO_GS_GLEVEL_FN = _fullFn( Out.O_INFO_GS_GLEVEL_FN )
    def EO_INFO_GS_GJSON_COMPAT_FN = _fullFn( Out.O_INFO_GS_GJSON_COMPAT_FN )
    def EO_INFO_GS_SHAPE_FN(ngl: MNodeGeoLevel) = _fullFn( Out.O_INFO_GS_SHAPE_FN(ngl) )

    // Гео-точки
    def EO_INFO_GEO_POINTS_FN = _fullFn( Out.O_INFO_GEO_POINTS_FN )

    // Внешние сервисы
    def EO_INFO_EXT_SERVICE_FN = _fullFn( Out.O_INFO_EXT_SERVICE_FN )
    def EO_INFO_OS_FAMILY_FN = _fullFn( Out.O_INFO_OS_FAMILY_FN )


    // Edge media
    def EO_MEDIA_FM_MIME_FN = _fullFn( Out.O_MEDIA_FM_MIME_FN )
    def EO_MEDIA_FM_MIME_AS_TEXT_FN = _fullFn( Out.O_MEDIA_FM_MIME_AS_TEXT_FN )

    /** Full FN nested-поля с хешами. */
    def EO_MEDIA_FM_HASHES_FN = _fullFn( Out.O_MEDIA_FM_HASHES_FN )
    def EO_MEDIA_FM_HASHES_TYPE_FN = _fullFn( Out.O_MEDIA_FM_HASHES_TYPE_FN )
    def EO_MEDIA_FM_HASHES_VALUE_FN = _fullFn( Out.O_MEDIA_FM_HASHES_VALUE_FN )

    def EO_MEDIA_FM_SIZE_B_FN = _fullFn( Out.O_MEDIA_FM_SIZE_B_FN )

    def EO_MEDIA_FM_IS_ORIGINAL_FN = _fullFn( Out.O_MEDIA_FM_IS_ORIGINAL_FN )

    def EO_MEDIA_STORAGE_TYPE_FN = _fullFn( Out.O_MEDIA_STORAGE_TYPE_FN )
    def EO_MEDIA_STORAGE_DATA_META_FN = _fullFn( Out.O_MEDIA_STORAGE_DATA_META_FN )
    def EO_MEDIA_STORAGE_DATA_SHARDS_FN = _fullFn( Out.O_MEDIA_STORAGE_DATA_SHARDS_FN )

    def EO_MEDIA_PICTURE_WH_WIDTH_FN = _fullFn( Out.O_MEDIA_PICTURE_WH_WIDTH_FN )
    def EO_MEDIA_PICTURE_WH_HEIGHT_FN = _fullFn( Out.O_MEDIA_PICTURE_WH_HEIGHT_FN )

  }


  /** Поля, относящиеся к биллингу. */
  object Billing extends PrefixedFn {

    /** Название корневого поля биллинга. */
    def BILLING_FN = "billing"
    override protected def _PARENT_FN = BILLING_FN

    def TARIFFS_DAILY_CURRENCY_FN       = _fullFn( MNodeTariffs.Fields.Daily.CURRENCY_FN )
    def TARIFFS_DAILY_CLAUSES_CAL_ID_FN = _fullFn( MNodeTariffs.Fields.Daily.CLAUSES_CAL_ID_FN )

    def BILLING_CONTRACT_ID_FN          = _fullFn( MNodeBilling.Fields.CONTRACT_ID_FN )

  }

}

