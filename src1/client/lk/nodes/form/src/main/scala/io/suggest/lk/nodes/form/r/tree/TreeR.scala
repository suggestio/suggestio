package io.suggest.lk.nodes.form.r.tree

import com.materialui.{Mui, MuiIconButton, MuiIconButtonProps, MuiLinearProgress, MuiLinearProgressProps, MuiLink, MuiLinkProps, MuiPaper, MuiProgressVariants, MuiTreeView, MuiTreeViewProps, MuiTypoGraphy, MuiTypoGraphyColors, MuiTypoGraphyProps, MuiTypoGraphyVariants}
import diode.react._
import diode.react.ReactPot._
import io.suggest.css.Css
import io.suggest.i18n.{MCommonReactCtx, MsgCodes}
import io.suggest.lk.nodes.form.m._
import io.suggest.lk.nodes.form.u.LknFormUtilR
import io.suggest.react.{ReactCommonUtil, ReactDiodeUtil}
import ReactDiodeUtil.Implicits._
import io.suggest.log.Log
import io.suggest.react.ReactCommonUtil.Implicits._
import io.suggest.spa.FastEqUtil
import io.suggest.ueq.UnivEqUtil._
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.scalajs.react._
import io.suggest.scalaz.ZTreeUtil._

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
             treeItemR            : TreeItemR,
             nodesDiConf          : NodesDiConf,
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

    private lazy val _onLoginClick = ReactCommonUtil.cbFun1ToJsCb { _: ReactEvent =>
      nodesDiConf.closeForm getOrElse Callback.empty
    }

    /** Рендер текущего компонента. */
    def render(p: Props, s: State): VdomElement = {
      <.div(
        ^.`class` := Css.flat(Css.Table.TABLE, Css.Table.Width.XL),

        // Рендерить узлы.
        s.root4nodeC { mrootProxy =>
          val mroot = mrootProxy.value

          val isAdvMode = mroot.conf.adIdOpt.nonEmpty
          val nodesPot = mroot.tree.tree.idsTree

          React.Fragment(

            // Когда ничего нет, то надо заполнить пустоту...
            nodesPot
              .filter { nodesTree =>
                // Дерево может быть пустым...
                !nodesTree
                  .subForest
                  .filter(!_.subForest.isEmpty)
                  .isEmpty
              }
              .renderEmpty {
                // Если юзер НЕ залогинен, то надо вывести предложение к логину.
                if (nodesPot.isPending) {
                  crCtxP.message( MsgCodes.`Please.wait` )

                } else if (!nodesDiConf.isUserLoggedIn()) {
                  // Юзер НЕ залогинен. Вывести приглашение к логину.
                  MuiLink(
                    new MuiLinkProps {
                      override val onClick = _onLoginClick
                      override val underline = MuiLink.Underline.ALWAYS
                    }
                  )(
                    nodesDiConf.needLogInVdom(),
                  )

                } else {
                  crCtxP.message( MsgCodes.`Nothing.found` )
                }
              },

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
                treeItemR.component(
                  p.resetZoom {
                    val treeIndexed = nodesTree.zipWithIndex
                    treeItemR.PropsVal(
                      subTreeIndexed = treeIndexed,
                      nodePathRev    = treeIndexed.rootLabel._2 :: Nil,
                      subTreeOrig    = nodesTree,
                      isAdvMode      = isAdvMode,
                      tree           = mroot.tree.tree,
                    )
                  }
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
          (a.tree.tree ===* b.tree.tree) &&
          (a.conf.adIdOpt ===* b.conf.adIdOpt)
        }),
      )
    }
    .renderBackend[Backend]
    .build

}
