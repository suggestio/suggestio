package io.suggest.adv.rcvr

import io.suggest.common.tree.NodesTreeWalkerSpecT
import io.suggest.dt.MYmd
import io.suggest.dt.interval.MRangeYmdOpt

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.02.17 14:30
  * Description: Тесты для рекурсивной модели [[MRcvrPopupNode]]
  */
object MRcvrPopupNodeSpec extends NodesTreeWalkerSpecT {

  override type Node_t = IRcvrPopupNode

  override type Nodes_t = IRcvrPopupNode.type
  override val Nodes_t = IRcvrPopupNode

  override val m1 = MRcvrPopupNode(
    id = "asdadasdsa",

    checkbox      = Some(MRcvrPopupMeta(
      isCreate    = true,
      checked     = false,
      name        = "test node",
      isOnlineNow = false,
      dateRange   = MRangeYmdOpt.empty
    )),

    subGroups = Seq(
      MRcvrPopupGroup(
        title = Some("abserare aedfad"),
        nodes = Seq(
          MRcvrPopupNode(
            id = "sub1-asdadasdsa",

            checkbox      = Some(MRcvrPopupMeta(
              isCreate    = false,
              checked     = true,
              name        = "test subnode",
              isOnlineNow = true,
              dateRange   = MRangeYmdOpt(
                Some(MYmd(2016,1,1)),
                Some(MYmd(2017,1,1))
              )
            )),

            subGroups = Seq(
              MRcvrPopupGroup(
                title = Some("sub-sub-group"),
                nodes = Seq(
                  MRcvrPopupNode(
                    id = "sub1-asdadasdsa",

                    checkbox      = Some(MRcvrPopupMeta(
                      isCreate    = false,
                      checked     = true,
                      name        = "test subnode",
                      isOnlineNow = true,
                      dateRange   = MRangeYmdOpt(
                        Some(MYmd(2016,1,1)),
                        Some(MYmd(2017,1,1))
                      )
                    )),

                    subGroups = Nil
                  )
                )
              )
            )
          ),

          MRcvrPopupNode(
            id = "sub2-asdadasdsa",

            checkbox      = Some(MRcvrPopupMeta(
              isCreate    = true,
              checked     = true,
              name        = "test subnode2",
              isOnlineNow = true,
              dateRange   = MRangeYmdOpt.empty
            )),

            subGroups = Seq(
              MRcvrPopupGroup(
                title = Some("sub-su22b-group"),
                nodes = Seq(
                  MRcvrPopupNode(
                    id = "sub22-asdadasdsa",

                    checkbox      = Some(MRcvrPopupMeta(
                      isCreate    = false,
                      checked     = true,
                      name        = "test subnode",
                      isOnlineNow = false,
                      dateRange   = MRangeYmdOpt(
                        Some(MYmd(2016,1,4)),
                        Some(MYmd(2018,6,3))
                      )
                    )),

                    subGroups = Nil
                  ),

                  MRcvrPopupNode(
                    id = "sub2233-asdadasdsa",

                    checkbox      = Some(MRcvrPopupMeta(
                      isCreate    = true,
                      checked     = true,
                      name        = "test subnode 22 33",
                      isOnlineNow = false,
                      dateRange   = MRangeYmdOpt(
                        Some(MYmd(2015,4,4)),
                        Some(MYmd(2019,6,3))
                      )
                    )),

                    subGroups = Nil
                  )
                )
              )
            )
          ),

          MRcvrPopupNode(
            id = "sub3-asdadasdsa",

            checkbox      = Some(MRcvrPopupMeta(
              isCreate    = true,
              checked     = true,
              name        = "test subnode3",
              isOnlineNow = true,
              dateRange   = MRangeYmdOpt.empty
            )),

            subGroups = Seq(
              MRcvrPopupGroup(
                title = Some("sub-sub-group33"),
                nodes = Seq(
                  MRcvrPopupNode(
                    id = "sub33-asdadasdsa",

                    checkbox      = Some(MRcvrPopupMeta(
                      isCreate    = false,
                      checked     = true,
                      name        = "test subnode3333",
                      isOnlineNow = false,
                      dateRange   = MRangeYmdOpt(
                        Some(MYmd(2016,1,4)),
                        Some(MYmd(2018,6,3))
                      )
                    )),

                    subGroups = Nil
                  )
                )
              )
            )
          )
        )
      )

    )
  )

}
