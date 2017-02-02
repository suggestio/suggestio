package io.suggest.adv.rcvr

import io.suggest.dt.MYmd
import io.suggest.dt.interval.MRangeYmdOpt
import minitest._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.02.17 14:30
  * Description: Тесты для рекурсивной модели [[MRcvrPopupNode]]
  */
object MRcvrPopupNodeSpec extends SimpleTestSuite {

  val m1 = MRcvrPopupNode(
    nodeId = "asdadasdsa",

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
            nodeId = "sub1-asdadasdsa",

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
                    nodeId = "sub1-asdadasdsa",

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
            nodeId = "sub2-asdadasdsa",

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
                    nodeId = "sub22-asdadasdsa",

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
                    nodeId = "sub2233-asdadasdsa",

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
            nodeId = "sub3-asdadasdsa",

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
                    nodeId = "sub33-asdadasdsa",

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


  test("findNode() for missing top-level node") {
    val r = IRcvrPopupNode.findNode(
      rcvrKey = "WHOOOAAA_SOMETHING_INVALID_ID" :: Nil,
      node    = m1
    )
    assertEquals(r, None)
  }

  test("findNode() for missing sub-node") {
    val r = IRcvrPopupNode.findNode(
      rcvrKey = "asdadasdsa" :: "WHOOOAAA_SOMETHING_INVALID_ID" :: Nil,
      node    = m1
    )
    assertEquals(r, None)
  }

  test("findNode() for completely missing sub-sub-sub-node") {
    val r = IRcvrPopupNode.findNode(
      rcvrKey = "asdadasdsa" :: "WHOOOAAA_SOMETHING_INVALID_ID" :: "ZZZZZZZZZZZZzz" :: "" :: Nil,
      node    = m1
    )
    assertEquals(r, None)
  }

  test("findNode() for completely missing inner-level node") {
    val r = IRcvrPopupNode.findNode(
      rcvrKey = "ZZZZZZZZZ_INVALID_ID" :: "WHOOOAAA_SOMETHING_INVALID_ID" :: "" :: Nil,
      node    = m1
    )
    assertEquals(r, None)
  }

  test("findNode() for top-level node") {
    val nodeId = "asdadasdsa"
    val rOpt = IRcvrPopupNode.findNode(
      rcvrKey = nodeId :: Nil,
      node    = m1
    )
    assert(rOpt.nonEmpty, rOpt.toString)
    val r = rOpt.get

    assertEquals(r.nodeId, nodeId)
  }

  test("findNode() for first existing sub-node") {
    val rcvrKey = "asdadasdsa" :: "sub1-asdadasdsa" :: Nil
    val rOpt = IRcvrPopupNode.findNode(
      rcvrKey = rcvrKey,
      node = m1
    )
    assert(rOpt.nonEmpty, rOpt.toString)
    val r = rOpt.get

    assertEquals(r.nodeId, rcvrKey.last)
  }

  test("findNode() for some deep non-first node") {
    val rcvrKey = "asdadasdsa" :: "sub2-asdadasdsa" :: "sub2233-asdadasdsa" :: Nil
    val rOpt = IRcvrPopupNode.findNode(
      rcvrKey = rcvrKey,
      node = m1
    )
    assert(rOpt.nonEmpty, rOpt.toString)
    val r = rOpt.get

    assertEquals(r.nodeId, rcvrKey.last)
  }

}
