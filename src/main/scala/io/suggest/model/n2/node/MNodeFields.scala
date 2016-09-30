package io.suggest.model.n2.node

import io.suggest.model._
import io.suggest.model.n2.FieldNamesL1
import io.suggest.model.n2.edge.MNodeEdges
import io.suggest.model.n2.geo.MNodeGeo
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.extra.MNodeExtras
import io.suggest.model.n2.node.meta.MMeta
import io.suggest.ym.model.NodeGeoLevel

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
    def COMMON_FN = FieldNamesL1.Common.name
    override protected def _PARENT_FN = COMMON_FN
    def NODE_TYPE_FN      = _fullFn( MNodeCommon.NODE_TYPE_FN )
    def IS_ENABLED_FN     = _fullFn( MNodeCommon.IS_ENABLED_FN )
    def IS_DEPENDENT_FN   = _fullFn( MNodeCommon.IS_DEPEND_FN )
  }

  /** Абсолютные имена ES-полей в .meta */
  object Meta extends PrefixedFn {
    /** Имя поля на стороне ES, куда скидываются все метаданные. */
    def META_FN                     = FieldNamesL1.Meta.name
    override protected def _PARENT_FN = META_FN

    import MMeta.{Fields => F}
    def BASIC_NAME_SHORT_NOTOK_FN   = _fullFn( F.Basic.NAME_SHORT_NOTOK_FN )
    def DATE_CREATED_FN             = _fullFn( F.Basic.DATE_CREATED_FN )
  }

  /** Абсолютные имена ES-полей в .extras */
  object Extras extends PrefixedFn {

    def EXTRAS_FN  = FieldNamesL1.Extras.name
    override protected def _PARENT_FN = EXTRAS_FN

    import MNodeExtras.{Fields => F}

    def ADN_IS_TEST_FN          = _fullFn( F.Adn.IS_TEST_FN )
    def ADN_RIGHTS_FN           = _fullFn( F.Adn.RIGHTS_FN )
    def ADN_SHOWN_TYPE_FN       = _fullFn( F.Adn.SHOWN_TYPE_FN )
    def ADN_SHOW_IN_SC_NL_FN    = _fullFn( F.Adn.SHOW_IN_SC_NL_FN )

    def BEACON_UUID_FN          = _fullFn( F.Beacon.UUID_FN )
    def BEACON_MAJOR_FN         = _fullFn( F.Beacon.MAJOR_FN )
    def BEACON_MINOR_FN         = _fullFn( F.Beacon.MINOR_FN )

    def DOMAIN_FN               = _fullFn( F.Domain.DOMAIN_FN )
    def DOMAIN_DKEY_FN          = _fullFn( F.Domain.DKEY_FN )
    def DOMAIN_MODE_FN          = _fullFn( F.Domain.MODE_FN )

  }

  /** Абсолютные имена ES-полей в .edges */
  object Edges extends PrefixedFn {

    def EDGES_FN = FieldNamesL1.Edges.name
    override protected def _PARENT_FN = EDGES_FN

    /** Адрес nested-объектов, хранящих данные по эджам. */
    def E_OUT_FN                   = _fullFn( MNodeEdges.Fields.OUT_FN )

    import MNodeEdges.Fields.Out

    def EDGE_OUT_PREDICATE_FULL_FN = _fullFn( Out.OUT_PREDICATE_FN )
    def EDGE_OUT_NODE_ID_FULL_FN   = _fullFn( Out.OUT_NODE_ID_FN )
    def EDGE_OUT_ORDER_FULL_FN     = _fullFn( Out.OUT_ORDER_FN )
    def EDGE_OUT_INFO_SLS_FN       = _fullFn( Out.OUT_INFO_SLS_FN )
    def EDGE_OUT_INFO_FLAG_FN      = _fullFn( Out.OUT_INFO_FLAG_FN )

    // Теги
    def E_OUT_INFO_TAGS_FN         = _fullFn( Out.OUT_INFO_TAGS_FN )
    def E_OUT_INFO_TAGS_RAW_FN     = _fullFn( Out.OUT_INFO_TAGS_RAW_FN )

    // Гео-шейпы
    def E_OUT_INFO_GS_FN                          = _fullFn( Out.OUT_INFO_GS_FN )
    def E_OUT_INFO_GS_GLEVEL_FN                   = _fullFn( Out.OUT_INFO_GS_GLEVEL_FN )
    def E_OUT_INFO_GS_GJSON_COMPAT_FN             = _fullFn( Out.OUT_INFO_GS_GJSON_COMPAT_FN )
    def E_OUT_INFO_GS_SHAPE_FN(ngl: NodeGeoLevel) = _fullFn( Out.OUT_INFO_GS_SHAPE_FN(ngl) )

    // Гео-точки
    def E_OUT_INFO_GEO_POINTS_FN                  = _fullFn( Out.OUT_INFO_GEO_POINTS_FN )

  }

  /** Абсолютные названия географических полей.*/
  object Geo extends PrefixedFn {

    def GEO_FN = FieldNamesL1.Geo.name
    override protected def _PARENT_FN = GEO_FN

    def POINT_FN = _fullFn( MNodeGeo.Fields.POINT_FN )

  }


  /** Поля, касающиеся рекламо-карточной стороны узла. */
  object Ad {

    /** Название корневого поля с контейнером данных рекламной карточки. */
    def AD_FN = FieldNamesL1.Ad.name

  }


  /** Поля, относящиеся к биллингу. */
  object Billing {

    /** Название корневого поля биллинга. */
    def BILLING_FN = FieldNamesL1.Billing.name

  }

}

