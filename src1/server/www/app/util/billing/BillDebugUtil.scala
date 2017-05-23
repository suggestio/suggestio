package util.billing

import java.time.{LocalDate, OffsetDateTime}

import com.google.inject.{Inject, Singleton}
import io.suggest.bill.price.dsl.IPriceDslTerm
import io.suggest.bin.ConvCodecs
import io.suggest.dt.YmdHelpersJvm
import io.suggest.mbill2.m.balance.MBalances
import io.suggest.mbill2.m.dbg.{MDbgKeys, MDebug, MDebugs}
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.typ.{MItemType, MItemTypes}
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.mbill2.m.order.MOrders
import io.suggest.mbill2.m.txn.{MTxn, MTxnTypes, MTxns}
import io.suggest.mbill2.util.effect.{RW, RWT, WT}
import io.suggest.pick.PickleUtil
import io.suggest.util.CompressUtilJvm
import io.suggest.util.logs.MacroLogsImpl
import models.mproj.ICommonDi

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.05.17 14:46
  * Description: Утиль для взаимодействия с отладкой биллинга.
  * Исходный Bill2Util сильно растолстел, поэтому debug-утиль будет размножаться тут.
  */
@Singleton
class BillDebugUtil @Inject() (
                                mDebugs          : MDebugs,
                                mItems           : MItems,
                                bill2Util        : Bill2Util,
                                mOrders          : MOrders,
                                mBalances        : MBalances,
                                mTxns            : MTxns,
                                compressUtilJvm  : CompressUtilJvm,
                                ymdHelpersJvm    : YmdHelpersJvm,
                                val mCommonDi    : ICommonDi
                              )
  extends MacroLogsImpl
{

  import mCommonDi.ec
  import mCommonDi.slick.profile.api._
  import compressUtilJvm.Implicits._
  import ymdHelpersJvm.Implicits._


  /** Сериализация инстанса PriceDSL в формат для хранения. */
  // TODO Добавить поддержку версий формата. Сюда или прямо в модель ключей.
  def priceDsl2bytes(priceTerm: IPriceDslTerm): Array[Byte] = {
    PickleUtil.pickleConv[IPriceDslTerm, ConvCodecs.Gzip, Array[Byte]](priceTerm)
  }

  // TODO Добавить поддержку версий формата. Сюда или прямо в модель ключей.
  def bytes2priceDsl(bytes: Array[Byte]): IPriceDslTerm = {
    PickleUtil.unpickleConv[Array[Byte], ConvCodecs.Gzip, IPriceDslTerm](bytes)
  }



  /** Сохранение отладки с инстансом priceDsl.
    *
    * @param objectId id элемента биллинга.
    * @param priceTerm Терм priceDsl.
    * @return DB-экшен, возвращающий кол-во добавленных рядов.
    */
  def savePriceDslDebug(objectId: Gid_t, priceTerm: IPriceDslTerm): DBIOAction[Int, NoStream, Effect.Write] = {
    val key = MDbgKeys.PriceDsl
    val mdbg0 = MDebug(
      objectId = objectId,
      key      = key,
      vsn      = key.V_CURRENT,
      data     = priceDsl2bytes(priceTerm)
    )
    mDebugs.insertOne( mdbg0 )
  }


  /** В *Adv*BillUtil.addToOrder() обычно требуется добавить item и алгоритм рассчёта его цены.
    * Тут метод, реализующий эти два дела.
    *
    * @param mitem Item для insert'а, без id обычно.
    * @param priceTerm Терм рассчёта цены.
    * @return DB-экшен, возвращающий сохранённый инстанс MItem.
    */
  def insertItemWithPriceDebug(mitem: MItem, priceTerm: IPriceDslTerm): DBIOAction[MItem, NoStream, WT] = {
    for {
      mItem2      <- mItems.insertOne(mitem)
      dbgCount    <- savePriceDslDebug( mItem2.id.get, priceTerm )
    } yield {
      LOGGER.trace(s"insertItemWithPriceDebug(): Item $mItem2 inserted with $dbgCount debugs")
      mItem2
    }
  }
  /** Кортежная реализация insertItemWithPriceDebug/2, которая обычно и нужна. */
  def insertItemWithPriceDebug(mitemTerm: (MItem, IPriceDslTerm)): DBIOAction[MItem, NoStream, WT] = {
    val (mitem, priceTerm) = mitemTerm
    insertItemWithPriceDebug(mitem, priceTerm)
  }


  /** Извлечь терм рассчёта стоимости для item'а.
    *
    * @param objectId id записи биллинга.
    * @return DB-экшен, опционально возвращающий терм рассчёта стоимости.
    */
  def getPriceDebug(objectId: Gid_t): DBIOAction[Option[IPriceDslTerm], NoStream, Effect.Read] = {
    val key = MDbgKeys.PriceDsl
    for {
      dbgOpt <- mDebugs.getByIdKey(objectId, key)
    } yield {
      dbgOpt.flatMap { dbg =>
        lazy val logPrefix = s"getPriceDebug($objectId):"

        if (dbg.vsn != key.V_CURRENT)
          LOGGER.warn(s"$logPrefix Unsupported vsn: ${dbg.vsn}, current vsn = ${key.V_CURRENT}")

        // Сюда можно впиливать код поддержки других версий бинарного API.
        // Попытаться десериализовать в текущем виде, даже если версия API не совпадает.
        try {
          val term = bytes2priceDsl(dbg.data)
          Some(term)
        } catch {
          case ex: Throwable =>
            LOGGER.error(s"$logPrefix Failed to deserialize debug information $dbg, failure suppressed.", ex)
            None
        }
      }
    }
  }


  /** "Прерывание" активного item'а означает его немедленное завершение с пересчётом стоимости
    * до сегодняшнего дня включительно.
    *
    * @param itemId id прерываемого item'а.
    * @param dateEnd Дата закрытия item'а.
    * @return Транзакционный DB-экшен, возвращающий обновлённый инстанс MItem.
    */
  def interruptItem(itemId: Gid_t, dateEnd: OffsetDateTime = OffsetDateTime.now()): DBIOAction[_, NoStream, RWT] = {
    lazy val logPrefix = s"interruptItem($itemId)[${System.currentTimeMillis()}]:"
    LOGGER.trace(s"$logPrefix Starting, dateEnd = $dateEnd ...")

    for {
      // Получить прерываемый item из базы, максимально заблокировав его.
      mitemOpt <- mItems.getById(itemId).forUpdate
      mitem = mitemOpt.get

      // Прерывать можно только online-item'ы.
      if {
        LOGGER.trace(s"$logPrefix Found item: $mitem")
        mitem.iType.isInterruptable &&
          (mitem.status == MItemStatuses.Online) &&
          // НЕ допускаем без даты окончания, т.к. это может быть нечто ошибочное.
          mitem.dateEndOpt.exists { _.isBefore(dateEnd) }
      }

      // Надо фильтрануть priceDsl по датам, не трогая остальных частей.
      priceDslOpt2: Option[IPriceDslTerm] <- {
        if (mitem.price.amount > 0) {
          // Это обычный item, оплаченный юзером. Имеет смысл поискать дебажную инфу и поковыряться в ней.
          for {
            mDebugOpt <- getPriceDebug(itemId)
          } yield {
            mDebugOpt.flatMap { priceDsl0 =>
              // Подготовить дату завершения к фильтрации.
              val dateEndLd = dateEnd.toLocalDate

              LOGGER.trace(s"$logPrefix DateEnd=$dateEndLd; found price debug: $priceDsl0")
              priceDsl0.filterDates {
                case Some(ymd) =>
                  val ld = ymd.to[LocalDate]
                  val isLdInFuture = ld.isAfter( dateEndLd )
                  // Допустимы только прошедшие даты до новой даты закрытия включительно.
                  val res = !isLdInFuture
                  if (!res)
                    LOGGER.trace(s"$logPrefix Dropped price date $ld, because it is in future (relative to $dateEndLd)")
                  res

                // Без даты. Возможно, это какая-то наценка просто сбоку, пропускаем.
                case None =>
                  LOGGER.trace(s"$logPrefix Skipped undefined date")
                  true
              }
            }
          }
        } else {
          // item с нулевой стоимостью. Возможно, это бесплатное размещение суперюзера.
          LOGGER.debug(s"$logPrefix Item price is too low to rollback money: ${mitem.price}. Will skip re-pricing.")
          DBIO.successful( None )
        }
      }

      // На руках есть обновлённый price-терм или None.
      // Если None, значит цену пересчитывать не надо, просто закрыть item.
      // Some(term), значит надо вычислить новую цену, залить разницу в деньгах, закрыть item с новым ценником.
      res <- {
        val currItemSql = mItems.query
          .filter( _.withIds(itemId))

        priceDslOpt2.fold [DBIOAction[_, NoStream, RW]] {
          LOGGER.debug(s"$logPrefix PriceDSL filtered into None. No money need to be returned to user. Keeping item price as-is.")
          // Обновить некоторые поля у item'а
          for {
            itemsUpdated <- {
              currItemSql
                .map { _.dateEndOpt }
                .update { Some(dateEnd) }
            }
            if itemsUpdated == 1
          } yield {
            LOGGER.trace(s"$logPrefix Just finished item ")
            itemsUpdated
          }
          DBIO.successful( None )

        } { priceDsl2 =>
          val price2 = priceDsl2.price
          LOGGER.debug(s"$logPrefix PriceDSL mutated from ${mitem.price} into $price2 . Will return money diff to user balance and update item price.")

          // Получить текущий баланс юзера по номеру контракта и валюте
          for {
            contractIdOpt <- mOrders.getContractId( mitem.orderId )
            contractId    = contractIdOpt.get
            bal0          <- bill2Util.ensureBalanceFor( contractId, mitem.price.currency )  // forUpdate уже внутри там

            // Вычислить разницу в цене
            diffAmount    = mitem.price.amount - price2.amount
            // Убедится, что новая цена не больше старой.
            if {
              val r = diffAmount >= 0   // Надо >, т.к. = отрабатывается в None-ветке, но чисто на всякий случай тут >=
              if (!r)
                throw new IllegalStateException(s"$logPrefix diffAmount must be >= 0, but it == $diffAmount, mitem = $mitem, price2 = $price2")
              r
            }

            // Залить разницу юзеру на баланс.
            bal2Opt <- mBalances.incrAmountBy( bal0, diffAmount )
            bal2 = bal2Opt.get

            // Сохранить транзакцию возврата денежных средств.
            _ <- {
              val mtxn0 = MTxn(
                balanceId       = bal2.id.get,
                amount          = diffAmount,
                txType          = MTxnTypes.InterruptPartialRefund,
                itemId          = Some( itemId )
              )
              mTxns.insertOne( mtxn0 )
            }

            // Наконец, обновить текущий item:
            itemsUpdated <- {
              currItemSql
                .map { i =>
                  (i.dateEndOpt, i.amount)
                }
                .update {
                  (Some(dateEnd), price2.amount)
                }
            }
            if itemsUpdated == 1

            // Обновить дебажные данные item'а новым алгоримом обсчёта обновлённого ценника.
            dbgsUpdated <- savePriceDslDebug(itemId, priceDsl2)
            if dbgsUpdated == 1

          } yield {
            // Всё выполнено ок, вернуть наверх что-нибудь.
            LOGGER.trace(s"$logPrefix Updated item, and returned $diffAmount ${price2.currency} into balance#${bal2.id.orNull} of contract#$contractId")
            itemsUpdated
          }
        }
      }

    } yield {
      LOGGER.trace(s"$logPrefix Finished.")
      res
    }
  }


  /** Найти и прервать онлайновые item'ы с указанными типами и node-id'шниками.
    * Такое бывает нужно при слепой перезаписи существующих item'ов.
    *
    * @param nodeIds id узлов, на которые указывают item'ы.
    * @param itypes Типы item'ов, которые требуется ускоренно завершить.
    *
    * @return Экшен обновления всего-всего необходимого для этого действа.
    */
  def findAndInterruptItemsLike(nodeIds: Traversable[String], itypes: MItemType*): DBIOAction[Int, NoStream, RWT] = {
    lazy val logPrefix = s"findAndInterruptItemsLike()[${System.currentTimeMillis()}]:"
    LOGGER.trace(s"$logPrefix Starting, nodeIds=[${nodeIds.mkString(",")}] ; itypes=[${itypes.mkString(",")}]")

    for {
      // Найти id'шники всех item'ов, которые подходят под заданные критерии:
      itemIds <- mItems.query
        .filter { i =>
          i.withNodeIds(nodeIds) &&
            i.withTypes(itypes) &&
            i.withStatus( MItemStatuses.Online ) &&
            i.withTypes( MItemTypes.interruptable )
        }
        .map(_.id)
        // Без limit, надо огулять все item'ы.
        .result
        .forUpdate

      // Завернуть каждый найденный item в соотв.экшен завершения item'а.
      itemsUpdated <- {
        if (itemIds.isEmpty) {
          LOGGER.trace(s"$logPrefix No items found for interruption.")
          DBIO.successful(0)

        } else {
          val itemIdsCount = itemIds.size
          LOGGER.info(s"$logPrefix Will interrupt $itemIdsCount items one-by-one: ${itemIds.mkString(", ")}")
          // Собрать db-экшены для каждого item'а.
          val dbActions = for {
            itemId <- itemIds
          } yield {
            interruptItem(itemId)
          }
          // Объеденить все собранные экшены, вернуть как бы итог.
          for {
            _ <- {
              DBIO.seq(dbActions: _*)
                .transactionally
            }
          } yield {
            LOGGER.trace(s"$logPrefix Interrupted $itemIdsCount items.")
            itemIdsCount
          }
        }
      }

    } yield {
      LOGGER.trace(s"$logPrefix Done, $itemsUpdated items udpated")
      itemsUpdated
    }
  }

}