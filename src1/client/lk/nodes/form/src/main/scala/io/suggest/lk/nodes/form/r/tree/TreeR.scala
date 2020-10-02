package io.suggest.lk.nodes.form.r.tree

import com.materialui.{Mui, MuiIconButton, MuiIconButtonProps, MuiLinearProgress, MuiLinearProgressProps, MuiPaper, MuiProgressVariants, MuiTreeItem, MuiTreeItemProps, MuiTreeView, MuiTreeViewProps, MuiTypoGraphy, MuiTypoGraphyColors, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import diode.react._
import diode.react.ReactPot._
import io.suggest.common.html.HtmlConstants._
import io.suggest.css.Css
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.nodes.form.m._
import io.suggest.lk.nodes.form.u.LknFormUtilR
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import ReactDiodeUtil.Implicits._
import io.suggest.log.Log
import io.suggest.msg.ErrorMsgs
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.scalaz.NodePath_t
import io.suggest.spa.FastEqUtil
import io.suggest.ueq.UnivEqUtil._
import io.suggest.ueq.JsUnivEqUtil._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import io.suggest.scalaz.ZTreeUtil._
import io.suggest.scalaz.ScalazUtil.Implicits._
import scalaz.Tree
import io.suggest.sjs.common.empty.JsOptionUtil.Implicits._

import scala.scalajs.js
import js.JSConverters._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 18:19
  * Description: React-компонент дерева узлов.
  * Каждый узел дерева описывается компонентом [[NodeR]].
  */
class TreeR(
             nodeR                : NodeR,
             crCtxP               : React.Context[MCommonReactCtx],
           )
  extends Log
{

  type Props = ModelProxy[MLkNodesRoot]


  case class State(
                    root4nodeC        : ReactConnectProxy[MLkNodesRoot],
                  )

  /** Ядро react-компонента дерева узлов. */
  class Backend($: BackendScope[Props, State]) {

    private lazy val _reloadClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, TreeInit() )
    }

    /** Рендер текущего компонента. */
    def render(p: Props, s: State): VdomElement = {
      <.div(
        ^.`class` := Css.flat(Css.Table.TABLE, Css.Table.Width.XL),

        // Рендерить узлы.
        s.root4nodeC { mrootProxy =>
          val mroot = mrootProxy.value
          val isAdvMode = mroot.conf.adIdOpt.nonEmpty

          /** Функция рекурсивного рендер всего дерева узлов.
            *
            * @param subTreeOrig Оригинальное поддерево из circuit-состояния с поддержкой целостности указателей.
            * @param subTreeIndexed Только что проиндексированное дерево.
            * @param parentNodePathRev Собранный наколенке reverse-путь до родительского элемента.
            * @return Отрендеренный VdomNode.
            */
          def _renderTreeNodeIndexed(
                                      subTreeOrig             : Tree[String],
                                      subTreeIndexed          : Tree[(String, Int)],
                                      parentNodePathRev       : NodePath_t,
                                      isAdvMode               : Boolean,
                                    ): VdomNode = {
            val (treeId, i) = subTreeIndexed.rootLabel

            val nodePathRev: NodePath_t = i :: parentNodePathRev
            val origSubForest = subTreeOrig.subForest

            val chsRendered = subTreeIndexed
              .subForest
              // Проходим оба subForest одновременно, чтобы иметь на руках сразу индексированное дерево, и неизменный указатель на оригинал.
              .zip( origSubForest )
              .map { case (chTreeIndexed, chTreeOrig) =>
                _renderTreeNodeIndexed(
                  subTreeIndexed      = chTreeIndexed,
                  parentNodePathRev   = nodePathRev,
                  subTreeOrig         = chTreeOrig,
                  isAdvMode           = isAdvMode,
                )
              }
              .iterator
              .toVdomArray

            mroot.tree.tree.nodesMap
              .get( treeId )
              .fold[VdomNode] {
                // Внутренняя ошибка: дерево id не соответствует карте состояний, и какой-то объявленный узел отсутствует.
                logger.error( ErrorMsgs.NODE_NOT_FOUND, msg = (treeId, mroot.tree.tree.nodesMap.keySet) )
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
                      rawNodePathRev = nodePathRev,
                    )

                    p.wrap { mroot =>
                      nodeR.PropsVal(
                        node          = mnsr,
                        advMode       = isAdvMode,
                        opened        = mroot.tree.tree.opened,
                        chs           = origSubForest,
                        nodesMap      = mroot.tree.tree.nodesMap,
                      )
                    } (
                      nodeR.component
                        .withKey( nodePathRev.mkString(`.`) )(_)( chsRendered )
                    )

                }
              }
          }

          val nodesPot = mroot.tree.tree.idsTree

          React.Fragment(

            // Дерево загружено в память, рендерим:
            nodesPot.render { nodesTree =>
              // выделить визуально надо только текущий открытый узел, если есть.
              val _selectedTreeNodeId = mroot.tree.tree.opened
                .map( LknFormUtilR.nodePath2treeId )

              // expanded: Надо всю цепочку expanded-узлов перечислять, а не только конечный узел:
              val _expandedTreeNodeIds = (for {
                opened  <- mroot.tree.tree.opened.iterator
                // .reverse: нужно хвост укорачивать с хвоста, а не с головы, поэтому реверсим исходник, затем реверсим обратно результаты:
                nthTail <- opened.reverse.tails
              } yield {
                LknFormUtilR.nodePath2treeId( nthTail.reverse )
              })
                .toJSArray

              MuiTreeView(
                new MuiTreeViewProps {
                  override val defaultCollapseIcon = Mui.SvgIcons.ExpandMore()().rawNode
                  override val defaultExpandIcon = Mui.SvgIcons.ChevronRight()().rawNode
                  // js.defined: Тут нельзя undefined, т.к. это приведёт к смене ошибочному controlled => uncontrolled и сворачиванию дерева.
                  // Явно фиксируем controlled-режим, чтобы не допускать логических ошибок на фоне желанию задействовать undefined:
                  override val expanded = js.defined( _expandedTreeNodeIds )
                  override val selected = js.defined( _selectedTreeNodeId.toJSArray )
                }
              )(
                _renderTreeNodeIndexed(
                  subTreeIndexed = nodesTree.zipWithIndex,
                  parentNodePathRev = Nil,
                  subTreeOrig    = nodesTree,
                  isAdvMode      = isAdvMode,
                )
              )
            },

            // Крутилка, когда дерево ещё пока загружается...
            nodesPot.renderPending { _ =>
              MuiLinearProgress(
                new MuiLinearProgressProps {
                  override val variant = MuiProgressVariants.indeterminate
                }
              )
            },

            // Рендер произошедшей ошибки.
            nodesPot.renderFailed { ex =>
              MuiPaper()(
                MuiTypoGraphy(
                  new MuiTypoGraphyProps {
                    override val variant = MuiTypoGraphyVariants.subtitle1
                    override val color = MuiTypoGraphyColors.textSecondary
                  }
                )(
                  crCtxP.message( MsgCodes.`Error` ),
                ),

                // Вывод сообщения об ошибке, если есть:
                Option( ex.getMessage )
                  .filter(_.nonEmpty)
                  .whenDefinedEl { errorMessage =>
                    React.Fragment(
                      <.br,
                      MuiTypoGraphy(
                        new MuiTypoGraphyProps {
                          override val variant = MuiTypoGraphyVariants.subtitle1
                          override val color = MuiTypoGraphyColors.error
                        }
                      )(
                        errorMessage,
                      ),
                      <.br,
                    )
                  },

                MuiIconButton(
                  new MuiIconButtonProps {
                    override val onClick = _reloadClick
                    override val disabled = nodesPot.isPending
                  }
                )(
                  Mui.SvgIcons.Refresh()(),
                ),
              )
            },

          )
        },

      )
    }

  }


  val component = ScalaComponent
    .builder[Props]( getClass.getSimpleName )
    .initialStateFromProps { propsProxy =>
      State(
        root4nodeC = propsProxy.connect(identity(_))( FastEqUtil[MLkNodesRoot] { (a, b) =>
          (a.tree.tree.opened ===* b.tree.tree.opened) &&
          (a.tree.tree.nodesMap ===* b.tree.tree.nodesMap) &&
          (a.tree.tree.idsTree ===* b.tree.tree.idsTree) &&
          (a.conf.adIdOpt ===* b.conf.adIdOpt)
        }),
      )
    }
    .renderBackend[Backend]
    .build

}
