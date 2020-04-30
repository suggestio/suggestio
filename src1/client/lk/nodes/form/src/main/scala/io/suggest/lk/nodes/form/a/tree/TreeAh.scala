package io.suggest.lk.nodes.form.a.tree

import diode._
import diode.data.{Pending, Pot}
import io.suggest.adn.edit.NodeEditConstants
import io.suggest.common.html.HtmlConstants
import io.suggest.lk.nodes.{MLknConf, MLknNode, MLknNodeReq}
import io.suggest.lk.nodes.form.a.ILkNodesApi
import io.suggest.lk.nodes.form.m._
import io.suggest.msg.ErrorMsgs
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.log.Log
import io.suggest.text.StringUtil
import monocle.Traversal
import scalaz.std.option._

import scala.util.Success

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 21:50
  * Description: Diode action-handler
  */
class TreeAh[M](
                 api          : ILkNodesApi,
                 modelRW      : ModelRW[M, MTree],
                 confRO       : ModelRO[MLknConf]
               )
  extends ActionHandler(modelRW)
  with Log
{

  /** Интерфейс typeclass'ов точечного обновления состояния. */
  private sealed trait OptStateUpdater[T] {
    def getStateOpt(mns0: MNodeState): Option[T]
    def updateState(mns0: MNodeState, data: Option[T]): MNodeState
  }

  implicit private object EditStateUpdater extends OptStateUpdater[MEditNodeState] {
    override def getStateOpt(mns0: MNodeState) = mns0.editing
    override def updateState(mns0: MNodeState, data: Option[MEditNodeState]) = mns0.withEditing( data )
  }


  private def _updateOptState[T](m: LkNodesTreeAction)(f: Option[T] => Option[T])(implicit osu: OptStateUpdater[T]) = {
    val v0 = value
    val nodes2 = MNodeState
      .flatMapSubNode(m.rcvrKey, v0.nodes) { mns0 =>
        val s0 = osu.getStateOpt(mns0)
        val s2 = f(s0)
        val mns2 = if (s0 eq s2) {
          // Ничего не изменилось. Просто вернуть исходный элемент.
          mns0
        } else {
          osu.updateState( mns0, s2 )
        }
        mns2 :: Nil
      }
      .toList

    val v2 = v0.withNodes( nodes2 )
    updated(v2)
  }

  private def _updateNameIn[T <: IEditNodeState[T]](m: LkNodesTreeNameAction)(implicit osu: OptStateUpdater[T]) = {
    _updateOptState[T](m) { addStateOpt0 =>
      val name2 = StringUtil.strLimitLen(
        str     = m.name,
        maxLen  = NodeEditConstants.Name.LEN_MAX,
        ellipsis = ""
      )
      val nameValid2 = name2.length >= NodeEditConstants.Name.LEN_MIN

      addStateOpt0.map(
        _.withName(name2, nameValid2)
      )
    }
  }


  override protected def handle: PartialFunction[Any, ActionResult[M]] = {

    // Сигнал о необходимости показать какой-то узел подробнее.
    case nnc: NodeNameClick =>
      val rcvrKey = nnc.rcvrKey

      val v0 = value
      MNodeState
        .findSubNode(rcvrKey, v0.nodes)
        .fold {
          logger.log( ErrorMsgs.NODE_NOT_FOUND, msg = nnc )
          noChange

        } { n =>
          if (n.children.nonEmpty) {
            // Дочерние элементы уже получены с сервера. Даже если их нет. Сфокусироваться на текущий узел либо свернуть его.
            // Узнать, является ли текущий элемент сфокусированным?
            if (v0.showProps.contains(rcvrKey)) {
              // Это был клик по заголовку текущего узла. Свернуть текущий узел.
              // Есть под-элементы. Свернуть подсписок, забыв их всех.
              val v2 = v0.copy(
                nodes = MNodeState
                  .flatMapSubNode(rcvrKey, v0.nodes) { mns0 =>
                    val mns1 = mns0.withChildren( Pot.empty )
                    mns1 :: Nil
                  }
                  .toList,
                showProps = None
              )
              updated(v2)

            } else {
              // Окликнутый узел не является сфокусированным, но для него уже загружены children. Выставить текущий узел как сфокусированный.
              val v2 = (MTree.showProps set Some(rcvrKey))(v0)
              updated(v2)
            }

          } else {
            // children.isEmpty: Для текущего узла не загружено children. Запустить в фоне загрузку, выставить её в состояние.
            val nodeId = nnc.rcvrKey.last

            // Собрать эффект запроса к серверу за подробностями по узлу.
            val fx = Effect {
              // Отправить запрос к серверу за данными по выбранному узлу, выставить ожидание ответа в состояние.
              api.nodeInfo(nodeId).transform { tryRes =>
                Success( HandleSubNodesOf(rcvrKey, tryRes) )
              }
            }

            // Произвести обновление модели.
            val v2 = v0.withNodes(
              MNodeState
                .flatMapSubNode(rcvrKey, v0.nodes) { mns0 =>
                  val mns1 = mns0.withChildren(
                    mns0.children.pending()
                  )
                  mns1 :: Nil
                }
                .toList
            )

            updated(v2, fx)
          }
        }


    // Ответ сервера на тему под-узлов.
    case snr: HandleSubNodesOf =>
      val v0 = value
      val v2 = value.copy(
        nodes = MNodeState
          .flatMapSubNode(snr.rcvrKey, v0.nodes) { mns0 =>
            val mns2 = snr.subNodesRespTry.fold(
              // Ошибка запроса. Сохранить её в состояние.
              {ex =>
                mns0.withChildren(
                  mns0.children.fail(ex)
                )
              },
              // Положительный ответ сервера, обновить данные по текущему узлу.
              {resp =>
                mns0.copy(
                  info      = resp.info,
                  children  = mns0.children.ready {
                    for (node <- resp.children) yield {
                      MNodeState(node)
                    }
                  }
                )
              }
            )
            mns2 :: Nil
          }
          .toList,
        showProps = Some(snr.rcvrKey)
      )
      updated(v2)


    // Юзер ткнул галочку размещения карточки на узле. Отправить запрос апдейта на сервер, обновив состояние узла.
    case m: AdvOnNodeChanged =>
      val conf = confRO()
      conf.adIdOpt.fold {
        // adId не задан в конфиге формы. Should never happen.
        logger.warn( ErrorMsgs.AD_ID_IS_EMPTY, msg = m + HtmlConstants.SPACE + conf )
        noChange

      } { adId =>
        val v0 = value
        MNodeState.findSubNode(m.rcvrKey, v0.nodes).fold {
          logger.log( ErrorMsgs.NODE_NOT_FOUND, msg = m )
          noChange
        } { mns0 =>
          if (mns0.adv.nonEmpty) {
            // Нельзя тыкать галочку, когда уже идёт обновление состояния на сервере.
            logger.log( ErrorMsgs.REQUEST_STILL_IN_PROGRESS, msg = (m, mns0) )
            noChange

          } else {
            // Всё ок, можно обновлять текущий узел и запускать реквест на сервер.
            val rcvrKey = m.rcvrKey

            // Организовать реквест на сервер.
            val fx = Effect {
              api.setAdv(
                adId      = adId,
                isEnabled = m.isEnabled,
                onNode    = rcvrKey
              ).transform { tryResp =>
                Success( AdvOnNodeResp(rcvrKey, tryResp) )
              }
            }

            // Обновить состояние формы.
            val v2 = value.withNodes(
              MNodeState
                .flatMapSubNode(rcvrKey, v0.nodes) { mns0 =>
                  val mns2 = mns0.withAdv(
                    Some( MNodeAdvState(
                      newIsEnabledPot = Pot.empty[Boolean].ready(m.isEnabled).pending()
                    ))
                  )
                  mns2 :: Nil
                }
                .toList
            )

            updated(v2, fx)
          }
        }
      }

    // Сигнал завершения реквеста изменения состояния размещения карточки на узле.
    case m: AdvOnNodeResp =>
      val v2 = MTree.nodes.modify { nodes0 =>
        MNodeState
          .flatMapSubNode(m.rcvrKey, nodes0) { mns0 =>
            val mns2 = m.tryResp.fold(
              {ex =>
                logger.error(ErrorMsgs.SRV_REQUEST_FAILED, ex, msg = m)
                MNodeState.adv
                  .composeTraversal( Traversal.fromTraverse[Option, MNodeAdvState] )
                  .composeLens( MNodeAdvState.newIsEnabledPot )
                  .modify(_.fail(ex))( mns0 )
              },
              {info2 =>
                (
                  MNodeState.info.set( info2 ) andThen
                  MNodeState.adv.set( None )
                )( mns0 )
              }
            )
            mns2 :: Nil
          }
          .toList
      }(value)
      updated(v2)


    // Положительный ответ сервера по поводу добавления нового узла.
    case m: CreateNodeResp =>
      m.tryResp.fold(
        // Ошибки с сервера отрабатываются в CreateNodeAh.
        {_ =>
          noChange
        },
        // Положительный ответ сервера: обновить дерево узлов.
        {resp =>
          // Залить обновления в дерево, удалить addState для текущего rcvrKey
          val v0 = value
          val v2 = MTree.nodes.modify { nodes0 =>
            MNodeState
              .flatMapSubNode(v0.showProps.get, nodes0) { mns0 =>
                val mns2 = MNodeState.children.set(
                  for {
                    children0 <- mns0.children
                  } yield {
                    val ch0 = MNodeState(
                      info = resp
                    )
                    children0 ++ (ch0 :: Nil)
                  }
                )(mns0)
                mns2 :: Nil
              }
              .toList
          }(v0)
          updated(v2)
        }
      )


    // Сигнал о клике юзером по галочке управления флагом isEnabled у какого-то узла.
    case m: NodeIsEnabledChanged =>
      val v0 = value
      val nodeOpt = MNodeState.findSubNode(m.rcvrKey, v0.nodes)
      nodeOpt
        .filter( _.info.canChangeAvailability.contains(true) )
        .fold {
          // Пришёл сигнал управления галочкой, но у юзера нет прав влиять на эту галочку.
          val errCode = if (nodeOpt.isDefined)
            ErrorMsgs.ACTION_WILL_BE_FORBIDDEN_BY_SERVER
          else
            ErrorMsgs.NODE_NOT_FOUND
          logger.warn( errCode, msg = m )

          // Без изменений, т.к. любые действия в данной ситуации бессмыслены.
          noChange

        } { _ =>
          // Выставить новое значение галочки в состояние узла, организовать реквест на сервер с апдейтом.
          val v2 = MTree.nodes.modify { nodes0 =>
            MNodeState
              .flatMapSubNode( m.rcvrKey, nodes0 ) { n0 =>
                val n2 = MNodeState.isEnableUpd.set(
                  Some( MNodeEnabledUpdateState(
                    newIsEnabled  = m.isEnabled,
                    request       = Pending()
                  ) )
                )(n0)
                n2 :: Nil
              }
              .toList
          }(v0)

          // Эффект апдейта на сервере:
          val fx = Effect {
            val rcvrKey = m.rcvrKey
            api
              .setNodeEnabled(rcvrKey.last, m.isEnabled)
              .transform { tryRes =>
                val r = NodeIsEnabledUpdateResp(rcvrKey, tryRes)
                Success(r)
              }
          }

          // Вернуть итог наверх...
          updated(v2, fx)
        }


    // Ответ сервер по поводу флага isEnabled для какого-то узла.
    case m: NodeIsEnabledUpdateResp =>
      val v0 = value
      val nodes2 = MNodeState
        .flatMapSubNode(m.rcvrKey, v0.nodes) { nodeState =>
          val nodeState2 = m.resp.fold [MNodeState] (
            // При ошибке запроса: сохранить ошибку в состояние
            {ex =>
              MNodeState.isEnableUpd
                .composeTraversal( Traversal.fromTraverse[Option, MNodeEnabledUpdateState] )
                .modify { nodeEnabledState =>
                  nodeEnabledState.copy(
                    newIsEnabled  = nodeState.info.isEnabled,
                    request       = nodeEnabledState.request.fail(ex)
                  )
                }(nodeState)
            },

            // Если всё ок, то обновить состояние текущего узла.
            {newNodeInfo =>
              nodeState.copy(
                info            = newNodeInfo,
                isEnabledUpd  = None
              )
            }
          )
          nodeState2 :: Nil
        }
        .toList

      val v2 = MTree.nodes.set(nodes2)(v0)
      updated(v2)


    // Сигнал о завершении запроса к серверу по поводу удаления узла.
    case m: NodeDeleteResp =>
      val v0 = value
      val v2 = MTree.nodes.modify { nodes0 =>
        MNodeState
          .flatMapSubNode(m.rcvrKey, nodes0)( MNodeState.deleteF )
          .toList
      }(v0)
      updated(v2)


    // Сигнал клика по кнопке редактирования узла.
    case m: NodeEditClick =>
      val v0 = value
      val v2 = v0.withNodes(
        MNodeState
          .flatMapSubNode( m.rcvrKey, v0.nodes ) { mns0 =>
            val mns2 = MNodeState.editing.set( Some(
              MEditNodeState(
                name = mns0.info.name,
                nameValid = true
              )
            ))(mns0)
            mns2 :: Nil
          }
          .toList
      )
      updated(v2)


    // Сигнал ввода нового имени узла.
    case m: NodeEditNameChange =>
      _updateNameIn[MEditNodeState](m)

    // Сигнал отмены редактирования узла.
    case m: NodeEditCancelClick =>
      _updateOptState[MEditNodeState](m) { _ => None }

    // Сигнал подтверждения редактирования узла.
    case m: NodeEditOkClick =>
      val v0 = value
      val rcvrKey = m.rcvrKey
      val n0 = MNodeState.findSubNode(rcvrKey, v0.nodes).get
      val editState0 = n0.editing.get

      if (editState0.isValid) {
        // Эффект обновления данных узла на сервере.
        val fx = Effect {
          val nodeId = rcvrKey.last
          val req = MLknNodeReq(
            name  = editState0.name.trim,
            id    = None
          )
          api
            .editNode(nodeId, req)
            .transform { tryResp =>
              val r = NodeEditSaveResp(rcvrKey, tryResp)
              Success(r)
            }
        }

        // Обновление состояния формы.
        val editState2 = editState0.withSavingPending()

        val v2 = v0.withNodes(
          MNodeState
            .flatMapSubNode(rcvrKey, v0.nodes) { mns0 =>
              val mns2 = MNodeState.editing.set( Some(editState2) )(mns0)
              mns2 :: Nil
            }
            .toList
        )
        updated(v2, fx)

      } else {
        noChange
      }


    // Сигнал завершения запроса сохранения с сервера.
    case m: NodeEditSaveResp =>
      val v0 = value
      val v2 = v0.withNodes(
        MNodeState
          .flatMapSubNode(m.rcvrKey, v0.nodes) { mns0 =>
            val mns2 = m.tryResp.fold[MNodeState](
              {ex =>
                MNodeState.editing
                  .composeTraversal( Traversal.fromTraverse[Option, MEditNodeState] )
                  .composeLens( MEditNodeState.saving )
                  .modify(_.fail(ex))(mns0)
              },
              {info2 =>
                (
                  MNodeState.editing.set( None ) andThen
                  MNodeState.info.set( info2 )
                )(mns0)
              }
            )
            mns2 :: Nil
          }
          .toList
      )
      updated(v2)


    // Юзер почитать развернуть подробности по тарифу.
    case m: TfDailyShowDetails =>
      val v2 = MTree.nodes.modify { nodes0 =>
        MNodeState
          .flatMapSubNode(m.rcvrKey, nodes0) { mns0 =>
            val mns2 = (MNodeState.tfInfoWide set true)(mns0)
            mns2 :: Nil
          }
          .toList
      }(value)
      updated( v2 )


    // Сигнал о завершении запроса к серверу на тему редактирования тарифа размещения.
    case m: TfDailySavedResp =>
      val v0 = value
      val rcvrKey = v0.showProps.get
      val v2 = MTree.nodes.modify { nodes0 =>
        MNodeState
          .flatMapSubNode( rcvrKey, nodes0 ) { mns0 =>
            val mns2 = m.tryResp.fold(
              {_ =>
                // Ошибка должна быть отработана EditTfDailyAh.
                mns0
              },
              {node2 =>
                MNodeState.info.set(node2)(mns0)
              }
            )
            mns2 :: Nil
          }
          .toList
      }(v0)

      updated(v2)


    // Реакция на изменение галочки showOpened напротив узла.
    case m: AdvShowOpenedChange =>
      val conf = confRO()
      conf.adIdOpt.fold {
        // adId не задан в конфиге формы. Should never happen.
        logger.warn( ErrorMsgs.AD_ID_IS_EMPTY, msg = m + HtmlConstants.SPACE + conf )
        noChange

      } { adId =>
        val v0 = value
        MNodeState.findSubNode(m.rcvrKey, v0.nodes).fold {
          logger.log( ErrorMsgs.NODE_NOT_FOUND, msg = m )
          noChange
        } { mns0 =>
          if (mns0.adv.nonEmpty) {
            // Нельзя тыкать галочку, когда уже идёт обновление состояния на сервере.
            logger.log( ErrorMsgs.REQUEST_STILL_IN_PROGRESS, msg = (m, mns0) )
            noChange

          } else {
            // Всё ок, можно обновлять текущий узел и запускать реквест на сервер.
            val rcvrKey = m.rcvrKey

            // Организовать реквест на сервер.
            val fx = Effect {
              api.setAdvShowOpened(
                adId          = adId,
                isShowOpened  = m.isChecked,
                onNode        = rcvrKey
              ).transform { tryResp =>
                Success( AdvShowOpenedChangeResp(m, tryResp) )
              }
            }

            // Обновить состояние формы.
            val v2 = MTree.nodes.modify { nodes0 =>
              MNodeState
                .flatMapSubNode(rcvrKey, nodes0) { mns0 =>
                  val mns2 = MNodeState.adv.set(
                    Some( MNodeAdvState(
                      isShowOpenedPot = Pot
                        .empty[Boolean]
                        .ready(m.isChecked)
                        .pending()
                    ))
                  )(mns0)
                  mns2 :: Nil
                }
                .toList
            }(v0)

            updated(v2, fx)
          }
        }
      }


    // Ответ сервера по запросу обновления галочки раскрытого
    case m: AdvShowOpenedChangeResp =>
      val v0 = value

      val v2 = MTree.nodes.modify { nodes0 =>
        MNodeState.flatMapSubNode(m.rcvrKey, nodes0) { nodeState =>
          val nodeState2 = m.tryResp.fold [MNodeState] (
            // При ошибке запроса: сохранить ошибку в состояние
            {ex =>
              MNodeState.adv
                .composeTraversal( Traversal.fromTraverse[Option, MNodeAdvState] )
                .composeLens( MNodeAdvState.isShowOpenedPot )
                .set( Pot.empty[Boolean].fail(ex) )(nodeState)
            },
            // Если всё ок, то обновить состояние текущего узла.
            {_ =>
              (
                MNodeState.adv.set(None) andThen
                MNodeState.info
                  .composeLens( MLknNode.advShowOpened )
                  .set( Some(m.reason.isChecked) )
              )( nodeState )
            }
          )
          nodeState2 :: Nil
        }
        .toList
      }(v0)

      updated(v2)


    // Реакция на изменение галочки showOpened напротив узла.
    case m: AlwaysOutlinedSet =>
      val conf = confRO()
      conf.adIdOpt.fold {
        // adId не задан в конфиге формы. Should never happen.
        logger.warn( ErrorMsgs.AD_ID_IS_EMPTY, msg = m + HtmlConstants.SPACE + conf )
        noChange

      } { adId =>
        val v0 = value
        MNodeState.findSubNode(m.rcvrKey, v0.nodes).fold {
          logger.log( ErrorMsgs.NODE_NOT_FOUND, msg = m )
          noChange
        } { mns0 =>
          if (mns0.adv.nonEmpty) {
            // Нельзя тыкать галочку, когда уже идёт обновление состояния на сервере.
            logger.log( ErrorMsgs.REQUEST_STILL_IN_PROGRESS, msg = (m, mns0) )
            noChange

          } else {
            // Всё ок, можно обновлять текущий узел и запускать реквест на сервер.
            val rcvrKey = m.rcvrKey

            // Организовать реквест на сервер.
            val fx = Effect {
              api.setAlwaysOutlined(
                adId          = adId,
                isAlwaysOutlined = m.isChecked,
                onNode        = rcvrKey
              ).transform { tryResp =>
                Success( AlwaysOutlinedResp(m, tryResp) )
              }
            }

            // Обновить состояние формы.
            val v2 = MTree.nodes.modify { nodes0 =>
              MNodeState
                .flatMapSubNode(rcvrKey, nodes0) { mns0 =>
                  val pot2 = Pot
                    .empty[Boolean]
                    .ready(m.isChecked)
                    .pending()
                  val mns2 = MNodeState.adv.set(
                    Some(
                      mns0.adv.fold {
                        MNodeAdvState(alwaysOutlinedPot = pot2)
                      } {
                        MNodeAdvState.alwaysOutlined.set(pot2)
                      }
                    )
                  )(mns0)
                  mns2 :: Nil
                }
                .toList
            }(v0)

            updated(v2, fx)
          }
        }
      }


    // Ответ сервера по запросу обновления галочки раскрытого
    case m: AlwaysOutlinedResp =>
      val v0 = value

      val v2 = MTree.nodes.modify { nodes0 =>
        MNodeState.flatMapSubNode(m.rcvrKey, nodes0) { nodeState =>
          val nodeState2 = m.tryResp.fold [MNodeState] (
            // При ошибке запроса: сохранить ошибку в состояние
            {ex =>
              MNodeState.adv
                .composeTraversal( Traversal.fromTraverse[Option, MNodeAdvState] )
                .composeLens( MNodeAdvState.alwaysOutlined )
                .set( Pot.empty[Boolean].fail(ex) )(nodeState)
            },
            // Если всё ок, то обновить состояние текущего узла.
            {_ =>
              (
                MNodeState.adv.set(None) andThen
                MNodeState.info
                  .composeLens( MLknNode.alwaysOutlined )
                  .set( Some(m.reason.isChecked) )
                )( nodeState )
            }
          )
          nodeState2 :: Nil
        }
          .toList
      }(v0)

      updated(v2)

  }

}
