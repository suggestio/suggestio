package io.suggest.lk.nodes.form.r.tree

import com.materialui.{Mui, MuiCircularProgress, MuiCircularProgressProps, MuiIconButton, MuiIconButtonProps, MuiPaper, MuiProgressVariants, MuiTreeView, MuiTreeViewProps, MuiTypoGraphy, MuiTypoGraphyColors, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import diode.react._
import diode.react.ReactPot._
import io.suggest.common.html.HtmlConstants.`.`
import io.suggest.css.Css
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.nodes.form.m._
import io.suggest.lk.nodes.form.u.LknFormUtilR
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
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
             nodeR      : NodeR,
             crCtxP     : React.Context[MCommonReactCtx],
           ) {

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

      /** Функция рекурсивного рендер всего дерева узлов. */
      def _renderTreeNodeIndexed(
                                  subTree                 : Tree[(MNodeState, Int)],
                                  parentNodePathRev       : NodePath_t,
                                ): VdomElement = {
        val (mns, i) = subTree.rootLabel
        val nodePathRev: NodePath_t = i :: parentNodePathRev
        val chs = subTree.subForest
        val chsRendered = chs
          .map { chTree =>
            _renderTreeNodeIndexed(
              subTree = chTree,
              parentNodePathRev = nodePathRev,
            )
          }
          .iterator
          .toVdomArray

        p.wrap { mroot =>
          nodeR.PropsVal(
            node = MNodeStateRender(
              state = mns,
              rawNodePathRev = nodePathRev,
              chCountEnabled = chs
                .iterator
                .count { chTree =>
                  chTree.rootLabel._1
                    .infoPot
                    .exists(_.isEnabled)
                },
              chCount = chs.length,
            ),
            confAdId      = mroot.conf.adIdOpt,
            opened        = mroot.tree.opened,
            locked        = mroot.popups.nonEmpty,
          )
        } (
          nodeR.component
            .withKey( nodePathRev.mkString(`.`) )(_)( chsRendered )
        )
      }

      <.div(
        ^.`class` := Css.flat(Css.Table.TABLE, Css.Table.Width.XL),

        // Рендерить узлы.
        s.root4nodeC { mrootProxy =>
          val mroot = mrootProxy.value

          val nodesPot = mroot.tree.nodes
          React.Fragment(

            // Крутилка, когда дерево ещё пока загружается...
            nodesPot.renderPending { _ =>
              MuiCircularProgress(
                new MuiCircularProgressProps {
                  override val variant = MuiProgressVariants.indeterminate
                }
              )
            },

            // Дерево загружено в память, рендерим:
            nodesPot.render { nodesTree =>
              // выделить визуально надо только текущий открытый узел, если есть.
              val _selectedTreeNodeId = mroot.tree.opened
                .map( LknFormUtilR.nodePath2treeId )

              // expanded: Надо всю цепочку expanded-узлов перечислять, а не только конечный узел:
              val _expandedTreeNodeIds = (for {
                opened  <- mroot.tree.opened.iterator
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
                  subTree = nodesTree.zipWithIndex,
                  parentNodePathRev = Nil,
                )
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
                <.br,

                MuiTypoGraphy(
                  new MuiTypoGraphyProps {
                    override val variant = MuiTypoGraphyVariants.subtitle1
                    override val color = MuiTypoGraphyColors.error
                  }
                )(
                  ex.getMessage,
                ),

                <.br,
                <.br,

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
          (a.tree.nodes ===* b.tree.nodes) &&
          (a.tree.opened ===* b.tree.opened) &&
          (a.conf.adIdOpt ===* b.conf.adIdOpt)
        }),
      )
    }
    .renderBackend[Backend]
    .build

}
