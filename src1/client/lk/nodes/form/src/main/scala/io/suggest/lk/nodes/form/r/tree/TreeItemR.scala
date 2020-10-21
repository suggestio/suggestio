package io.suggest.lk.nodes.form.r.tree

import com.materialui.{MuiTreeItem, MuiTreeItemProps, MuiTypoGraphy, MuiTypoGraphyColors, MuiTypoGraphyProps}
import diode.react.ModelProxy
import io.suggest.common.html.HtmlConstants._
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.nodes.form.m.{MNodeStateRender, MTree, MTreeRoles}
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.react.ReactDiodeUtil
import io.suggest.react.ReactDiodeUtil.Implicits._
import io.suggest.scalaz.NodePath_t
import io.suggest.scalaz.ScalazUtil.Implicits._
import io.suggest.scalaz.ZTreeUtil.zTreeUnivEq
import io.suggest.spa.FastEqUtil
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._
import scalaz.Tree

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.10.2020 21:07
  * Description: Элемент дерева с рендером всего поддерева.
  */
class TreeItemR(
                 nodeR                : NodeR,
                 crCtxP               : React.Context[MCommonReactCtx],
               )
  extends Log
{

  /** Пропертисы компонента.
    *
    * @param subTreeOrig Оригинальное поддерево из circuit-состояния с поддержкой целостности указателей.
    * @param subTreeIndexed Только что проиндексированное дерево.
    * @param nodePathRev Собранный наколенке reverse-путь до текущего элемента.
    * @param isAdvMode Режим размещения?
    * @param tree Контейнер данных дерева.
    */
  case class PropsVal(
                       subTreeOrig             : Tree[String],
                       subTreeIndexed          : Tree[(String, Int)],
                       nodePathRev             : NodePath_t,
                       isAdvMode               : Boolean,
                       tree                    : MTree,
                     )

  implicit val propsValFeq = FastEqUtil[PropsVal] { (a, b) =>
    (a.subTreeOrig ===* b.subTreeOrig) &&
    // subTreeIndexed пересобирается при каждом рендере, но собирается только на базе subTreeOrig, поэтому просто игнорируем subTreeIndexed.
    //(a.subTreeIndexed ===* b.subTreeIndexed) &&
    (a.tree ===* b.tree) &&
    (a.isAdvMode ==* b.isAdvMode) &&
    // Инстанс nodePathRev пересобирается при каждом рендере, но он небольшой - сравниваем напрямую.
    (a.nodePathRev ==* b.nodePathRev)
  }


  type Props_t = PropsVal
  type Props = ModelProxy[Props_t]


  class Backend( $: BackendScope[Props, Props_t] ) {
    def render(p: Props, s: Props_t): VdomNode = {
      val treeId = s.subTreeIndexed.rootLabel._1
      val origSubForest = s.subTreeOrig.subForest

      val chsRendered = s.subTreeIndexed
        .subForest
        // Проходим оба subForest одновременно, чтобы иметь на руках сразу индексированное дерево, и неизменный указатель на оригинал.
        .zip( origSubForest )
        .map { case (chTreeIndexed, chTreeOrig) =>
          val chNodePathRev: NodePath_t = chTreeIndexed.rootLabel._2 :: s.nodePathRev
          val p2 = p.resetZoom(
            s.copy(
              subTreeIndexed    = chTreeIndexed,
              nodePathRev       = chNodePathRev,
              subTreeOrig       = chTreeOrig,
            )
          )
          component.withKey( chNodePathRev.mkString(`.`) )( p2 ): VdomElement
        }
        .iterator
        .toVdomArray

      s.tree.nodesMap
        .get( treeId )
        .fold[VdomNode] {
          // Внутренняя ошибка: дерево id не соответствует карте состояний, и какой-то объявленный узел отсутствует.
          logger.error( ErrorMsgs.NODE_NOT_FOUND, msg = (treeId, s.tree.nodesMap.keySet) )
          MuiTreeItem(
            new MuiTreeItemProps {
              override val nodeId = treeId
            }
          )(
            MuiTypoGraphy(
              new MuiTypoGraphyProps {
                override val color = MuiTypoGraphyColors.error
              }
            )(
              crCtxP.message( MsgCodes.`Error` ),
              SPACE,
              DIEZ, treeId,
            ),

            chsRendered,
          )
        } { mns =>
          // Рендер подветви дерева:
          mns.role match {

            // Единственный корневой элемент: пропуск рендера, переход на следующий уровень.
            case MTreeRoles.Root =>
              chsRendered

            // Обычный узел - рендерим через NodeR()
            case _ =>
              // Рендер treeItem'а:
              val mnsr = MNodeStateRender(
                state = mns,
                rawNodePathRev = s.nodePathRev,
              )

              val p2 = p.resetZoom {
                nodeR.PropsVal(
                  node          = mnsr,
                  advMode       = s.isAdvMode,
                  opened        = s.tree.opened,
                  chs           = origSubForest,
                  nodesMap      = s.tree.nodesMap,
                )
              }

              nodeR
                .component
                .withKey( s.nodePathRev.mkString(`.`) )(p2)( chsRendered )

          }
        }
    }
  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps( ReactDiodeUtil.modelProxyValueF )
    .renderBackend[Backend]
    .configure( ReactDiodeUtil.statePropsValShouldComponentUpdate(propsValFeq) )
    .build

}
