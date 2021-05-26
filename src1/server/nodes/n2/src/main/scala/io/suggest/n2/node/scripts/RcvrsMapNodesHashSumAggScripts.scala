package io.suggest.n2.node.scripts

import io.suggest.es.scripts.{DocHashSumsAggScripts, IAggScripts}
import io.suggest.n2.edge.MPredicates
import io.suggest.n2.node.MNodeFields
import org.elasticsearch.script.Script

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.18 22:28
  * Description: Контейнер ES-скриптов для корректного рассчёта хэша состояния карты узлов.
  * Используется для рассчёта кэширования и де-кэширования.
  */
class RcvrsMapNodesHashSumAggScripts extends DocHashSumsAggScripts {

  import IAggScripts._
  import DocHashSumsAggScripts._

  import MNodeFields.Meta.META_FN
  import io.suggest.n2.node.meta.MMeta.Fields.Basic.BASIC_FN
  import io.suggest.n2.node.meta.MBasicMeta.Fields.NAME_FN
  import MNodeFields.Edges.EDGES_FN
  import io.suggest.n2.edge.MNodeEdges.Fields.OUT_FN
  import io.suggest.n2.edge.MEdge.Fields.{PREDICATE_FN, INFO_FN, NODE_ID_FN, ORDER_FN}
  import io.suggest.n2.edge.MEdgeInfo.Fields.GEO_POINT_FN


  /** Главный скрипт обработки полей MNode-документа, которые используются для сборки карты ресиверов. */
  override def mapScript: Script = {
    // TODO Надо пройтись по ОПЦИОНАЛЬНЫМ полям: название
    val DOC = s"$PARAMS.$SOURCE"

    /*
     * Для карты ресиверов важно:
     * - отображаемое название узла
     * - гео-точки для отображения логотипа
     * - НЕ НУЖЕН гео-шейп. Да, шейп отображается на карте, но зачем он для кэширования?
     *   Шейп можно смело игнорить, т.к. чисто-декоративная вещь на гео-карте клиента.
      |        for (gs in e.$INFO_FN.$GEO_SHAPES_FN) {
      |          hh = hh*31 + gs.${Gs.TYPE_ESFN}.hashCode();
      |          if (gs.${Gs.COORDS_ESFN}) {
      |            hh = gs*31 + gs.${Gs.COORDS_ESFN}.hashCode();
      |          }
      |          if (gs.${Gs.RADIUS_ESFN}) {
      |            hh = gs*31 + gs.${Gs.RADIUS_ESFN}.hashCode();
      |          }
      |          // gs.geometries: Не используется сейчас, и пропускается тут. Нужна рекурсия обработки одного шейпа.
      |         }
     * - Эдж логотипа/приветствия узла.
     *   Эджи галереи игнорим с помощью запрета значения sort/order, но для точности в будущем можно заглядывать в содержимое extras.adn.MAdnResView.
     * - При переборе эджей - игнорируем порядок эджей (отсутствует hh=hh*31+...), т.к. эджи часто перетасовываются.
     */
    val code = s"""
      |int hh = 0;
      |
      |if ($DOC.$META_FN.$BASIC_FN != null) {
      |  if ($DOC.$META_FN.$BASIC_FN.$NAME_FN != null) {
      |    hh += $DOC.$META_FN.$BASIC_FN.$NAME_FN.hashCode();
      |  }
      |}
      |
      |if ($DOC.$EDGES_FN != null && $DOC.$EDGES_FN.$OUT_FN != null && !$DOC.$EDGES_FN.$OUT_FN.isEmpty()) {
      |  for (e in $DOC.$EDGES_FN.$OUT_FN) {
      |    int eh = 0;
      |    List preds = e.$PREDICATE_FN;
      |
      |    if ( preds.contains("${MPredicates.NodeLocation.value}") ) {
      |      eh += preds.hashCode();
      |
      |      if ( e.$INFO_FN != null && e.$INFO_FN.$GEO_POINT_FN != null) {
      |        eh = eh*31 + e.$INFO_FN.$GEO_POINT_FN.hashCode();
      |      }
      |    }
      |
      |    if ( preds.contains("${MPredicates.JdContent.Image.value}") && e["$ORDER_FN"] == null ) {
      |      eh += preds.hashCode();
      |      if ( e.$NODE_ID_FN != null ) {
      |        eh = eh*31 + e.$NODE_ID_FN.hashCode();
      |      }
      |    }
      |
      |    hh += eh;
      |  }
      |}
      |
      |$STATE.$HASHES.add(hh);
    """.stripMargin

    new Script(code)
  }

}
