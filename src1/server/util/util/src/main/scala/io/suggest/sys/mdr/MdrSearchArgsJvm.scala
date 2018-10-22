package io.suggest.sys.mdr

import io.suggest.common.empty.OptionUtil
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.typ.MItemType
import io.suggest.model.play.qsb.{QsbSeq, QueryStringBindableImpl}
import play.api.mvc.QueryStringBindable
import io.suggest.common.empty.OptionUtil.BoolOptOps

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.06.14 16:39
 * Description: Модель для представления данных запроса поиска карточек в контексте s.io-пост-модерации.
 */
object MdrSearchArgsJvm {

  /** Поддержка qs-биндинга для [[MdrSearchArgs]]. */
  implicit def mdrSearchArgsQsb(implicit
                                strOptB     : QueryStringBindable[Option[String]],
                                intOptB     : QueryStringBindable[Option[Int]],
                                boolOptB    : QueryStringBindable[Option[Boolean]],
                                mdrConfB    : QueryStringBindable[MMdrConf],
                               ): QueryStringBindable[MdrSearchArgs] = {
    new QueryStringBindableImpl[MdrSearchArgs] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MdrSearchArgs]] = {
        val k1 = key1F(key)
        val F = MdrSearchArgs.Fields
        for {
          offsetOptE            <- intOptB.bind     (k1(F.OFFSET_FN),                 params)
          freeAdvIsAllowedE     <- boolOptB.bind    (k1(F.FREE_ADV_IS_ALLOWED_FN),    params)
          hideAdIdOptE          <- strOptB.bind     (k1(F.HIDE_AD_ID_FN),             params)
          mdrConfE              <- mdrConfB.bind    (k1(F.MDR_CONF_FN),               params)
        } yield {
          for {
            offsetOpt           <- offsetOptE.right
            freeAdvIsAllowed    <- freeAdvIsAllowedE.right
            hideAdIdOpt         <- hideAdIdOptE.right
            mdrConf             <- mdrConfE.right
          } yield {
            MdrSearchArgs(
              offsetOpt         = offsetOpt,
              isAllowed         = freeAdvIsAllowed,
              hideAdIdOpt       = hideAdIdOpt,
              conf              = mdrConf
            )
          }
        }
      }

      override def unbind(key: String, value: MdrSearchArgs): String = {
        val k1 = key1F(key)
        val F = MdrSearchArgs.Fields
        _mergeUnbinded1(
          intOptB.unbind        (k1(F.OFFSET_FN),               value.offsetOpt   ),
          boolOptB.unbind       (k1(F.FREE_ADV_IS_ALLOWED_FN),  value.isAllowed   ),
          strOptB.unbind        (k1(F.HIDE_AD_ID_FN),           value.hideAdIdOpt ),
          mdrConfB.unbind       (k1(F.MDR_CONF_FN),             value.conf        ),
        )
      }

    }
  }


  /** Поддержка qs-биндинга [[io.suggest.sys.mdr.MMdrActionInfo]]. */
  implicit def mdrActionInfoQsb(implicit
                                gidOptB         : QueryStringBindable[Option[Gid_t]],
                                itemTypeOptB    : QueryStringBindable[Option[MItemType]],
                                boolOptB        : QueryStringBindable[Option[Boolean]],
                                strOptB         : QueryStringBindable[Option[String]],
                               ): QueryStringBindable[MMdrActionInfo] = {
    new QueryStringBindableImpl[MMdrActionInfo] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MMdrActionInfo]] = {
        val k = key1F(key)
        val F = MMdrActionInfo.Fields
        for {
          itemIdOptE          <- gidOptB.bind       ( k(F.ITEM_ID_FN),          params )
          itemTypeOptE        <- itemTypeOptB.bind  ( k(F.ITEM_TYPE_FN),        params )
          directSelfAllOptE   <- boolOptB.bind      ( k(F.DIRECT_SELF_ALL_FN),  params )
          directSelfIdOptE    <- strOptB.bind       ( k(F.DIRECT_SELF_ID_FN),   params )
        } yield {
          for {
            itemIdOpt         <- itemIdOptE.right
            itemTypeOpt       <- itemTypeOptE.right
            directSelfAllOpt  <- directSelfAllOptE.right
            directSelfIdOpt   <- directSelfIdOptE.right
          } yield {
            MMdrActionInfo(
              itemId          = itemIdOpt,
              itemType        = itemTypeOpt,
              directSelfAll   = directSelfAllOpt.getOrElseFalse,
              directSelfId    = directSelfIdOpt
            )
          }
        }
      }

      override def unbind(key: String, value: MMdrActionInfo): String = {
        val k = key1F(key)
        val F = MMdrActionInfo.Fields
        _mergeUnbinded1(
          gidOptB     .unbind( k(F.ITEM_ID_FN),         value.itemId ),
          itemTypeOptB.unbind( k(F.ITEM_TYPE_FN),       value.itemType ),
          boolOptB    .unbind( k(F.DIRECT_SELF_ALL_FN), OptionUtil.maybeTrue(value.directSelfAll) ),
          strOptB     .unbind( k(F.DIRECT_SELF_ID_FN),  value.directSelfId )
        )
      }

    }
  }


  /** QS-биндер для [[io.suggest.sys.mdr.MMdrResolution]]. */
  implicit def mdrResolutionQsb(implicit
                                strB                : QueryStringBindable[String],
                                mdrActionInfoB      : QueryStringBindable[MMdrActionInfo],
                                strOptB             : QueryStringBindable[Option[String]],
                                mdrConfB            : QueryStringBindable[MMdrConf],
                               ): QueryStringBindable[MMdrResolution] = {
    new QueryStringBindableImpl[MMdrResolution] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MMdrResolution]] = {
        val k = key1F(key)
        val F = MMdrResolution.Fields
        for {
          nodeIdE       <- strB.bind            ( k(F.NODE_ID_FN),      params )
          infoE         <- mdrActionInfoB.bind  ( k(F.INFO_FN),         params )
          reasonOptE    <- strOptB.bind         ( k(F.REASON_FN),       params )
          mdrConfE      <- mdrConfB.bind        ( k(F.MDR_CONF_FN),     params )
        } yield {
          for {
            nodeId      <- nodeIdE.right
            info        <- infoE.right
            reasonOpt   <- reasonOptE.right
            mdrConf     <- mdrConfE.right
          } yield {
            MMdrResolution(
              nodeId    = nodeId,
              info      = info,
              reason    = reasonOpt,
              conf      = mdrConf
            )
          }
        }
      }

      override def unbind(key: String, value: MMdrResolution): String = {
        val k = key1F(key)
        val F = MMdrResolution.Fields
        _mergeUnbinded1(
          strB.unbind           ( k(F.NODE_ID_FN),      value.nodeId ),
          mdrActionInfoB.unbind ( k(F.INFO_FN),         value.info ),
          strOptB.unbind        ( k(F.REASON_FN),       value.reason ),
          mdrConfB.unbind       ( k(F.MDR_CONF_FN),     value.conf ),
        )
      }

    }
  }


  /** Поддержка биндинга инстансов MMdrConf. */
  implicit def mdrConfQsb(implicit
                          boolB             : QueryStringBindable[Boolean],
                          qsbSeqStrOptB     : QueryStringBindable[Option[QsbSeq[String]]],
                         ): QueryStringBindable[MMdrConf] = {
    new QueryStringBindableImpl[MMdrConf] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MMdrConf]] = {
        val k = key1F(key)
        val F = MMdrConf.Fields
        for {
          isSysE          <- boolB.bind( k(F.IS_SU_FN), params )
          onNodeKeyOptE   <- qsbSeqStrOptB.bind( k(F.ON_NODE_KEY_FN), params )
        } yield {
          for {
            isSys         <- isSysE.right
            onNodeKeyOpt  <- onNodeKeyOptE.right
          } yield {
            MMdrConf(
              isSu       = isSys,
              onNodeKey   = onNodeKeyOpt.map(_.items.toList)
            )
          }
        }
      }

      override def unbind(key: String, value: MMdrConf): String = {
        val k = key1F( key )
        val F = MMdrConf.Fields
        _mergeUnbinded1(
          boolB.unbind          ( k(F.IS_SU_FN),         value.isSu ),
          qsbSeqStrOptB.unbind  ( k(F.ON_NODE_KEY_FN),    value.onNodeKey.map(QsbSeq.apply) ),
        )
      }

    }
  }


}
