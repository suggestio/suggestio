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

    def BASIC_NAME_SHORT_NOTOK_FN   = _fullFn( MMeta.Fields.Basic.NAME_SHORT_NOTOK_FN )
    def DATE_CREATED_FN             = _fullFn( MMeta.Fields.Basic.DATE_CREATED_FN )
  }

  /** Абсолютные имена ES-полей в .extras */
  object Extras extends PrefixedFn {

    def EXTRAS_FN  = FieldNamesL1.Extras.name
    override protected def _PARENT_FN = EXTRAS_FN

    def ADN_IS_TEST_FN          = _fullFn( MNodeExtras.Fields.Adn.IS_TEST_FN )
    def ADN_RIGHTS_FN           = _fullFn( MNodeExtras.Fields.Adn.RIGHTS_FN )
    def ADN_SHOWN_TYPE_FN       = _fullFn( MNodeExtras.Fields.Adn.SHOWN_TYPE_FN )
    def ADN_SHOW_IN_SC_NL_FN    = _fullFn( MNodeExtras.Fields.Adn.SHOW_IN_SC_NL_FN )

    def TAG_FACE_NAME_FN        = _fullFn( MNodeExtras.Fields.Tag.FACE_NAME_FN )
    def TAG_FACES_FN            = _fullFn( MNodeExtras.Fields.Tag.FACES_FN )
  }

  /** Абсолютные имена ES-полей в .edges */
  object Edges extends PrefixedFn {

    def EDGES_FN = FieldNamesL1.Edges.name
    override protected def _PARENT_FN = EDGES_FN

    /** Адрес nested-объектов, хранящих данные по эджам. */
    def EDGES_OUT_FULL_FN = _fullFn( MNodeEdges.Fields.OUT_FN )

    import MNodeEdges.Fields.Out._

    def EDGE_OUT_PREDICATE_FULL_FN = _fullFn( OUT_PREDICATE_FN )
    def EDGE_OUT_NODE_ID_FULL_FN   = _fullFn( OUT_NODE_ID_FN )
    def EDGE_OUT_ORDER_FULL_FN     = _fullFn( OUT_ORDER_FN )
    def EDGE_OUT_INFO_SLS_FN       = _fullFn( OUT_INFO_SLS_FN )
    def EDGE_OUT_INFO_FLAG_FN      = _fullFn( OUT_INFO_FLAG_FN )
  }

  /** Абсолютные названия географических полей.*/
  object Geo extends PrefixedFn {

    def GEO_FN = FieldNamesL1.Geo.name
    override protected def _PARENT_FN = GEO_FN

    def POINT_FN = _fullFn( MNodeGeo.Fields.POINT_FN )

    def SHAPE_FN = _fullFn( MNodeGeo.Fields.Shape.SHAPE_FN )
    def SHAPE_GLEVEL_FN = _fullFn( MNodeGeo.Fields.Shape.SHAPE_GLEVEL_FN )

    def GEO_JSON_COMPATIBLE_FN = _fullFn( MNodeGeo.Fields.Shape.GEO_JSON_COMPATIBLE_FN )

    /** Абсолютное имя shape-поля указанного уровня в SHAPE_FN. */
    def geoShapeFn(ngl: NodeGeoLevel): String = {
      // TODO Тут дублируется часть логики SHAPE_FN, могут быть ошибки и есть возможности для оптимизации.
      _fullFn( MNodeGeo.Fields.Shape.geoShapeFullFn(ngl) )
    }

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

