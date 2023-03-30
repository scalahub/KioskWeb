package kiosk.wallet

import kiosk.appkit.Client
import kiosk.box.KioskBoxCreator
import kiosk.tx.TxUtil
import kiosk.ergo
import kiosk.ergo.{Amount, DhtData, ID, KioskBigInt}
import kiosk.explorer.Explorer
import jde.compiler.{Compiler => TxBuilder}
import jde.parser.Parser
import org.ergoplatform.ErgoAddressEncoder
import org.ergoplatform.appkit.{ConstantsBuilder, InputBox}
import org.sh.easyweb.Text
import org.sh.reflect.DataStructures.EasyMirrorSession
import play.api.libs.json.{JsValue, Json}
import scorex.crypto.hash.Blake2b256
import sigmastate.eval._
import sigmastate.interpreter.CryptoConstants
import special.sigma.GroupElement

class KioskWallet(val $ergoBox: KioskBoxCreator) extends EasyMirrorSession {
  private val explorer = new Explorer

  val secretKey: BigInt = BigInt(
    Blake2b256(
      $ergoBox.$ergoScript.$env.$sessionSecret.getOrElse("none").getBytes("UTF-16")
    )
  )
  private val defaultGenerator: GroupElement =
    CryptoConstants.dlogGroup.generator
  private val publicKey: GroupElement =
    defaultGenerator.exp(secretKey.bigInteger)
  val myAddress: String = {
    Client.usingContext { implicit ctx =>
      val contract = ctx.compileContract(
        ConstantsBuilder
          .create()
          .item(
            "gZ",
            publicKey
          )
          .build(),
        "proveDlog(gZ)"
      )
      val addressEncoder =
        new ErgoAddressEncoder(ctx.getNetworkType.networkPrefix)
      addressEncoder.fromProposition(contract.getErgoTree).get.toString
    }
  }

  def balance = {
    val boxes = explorer.getUnspentBoxes(myAddress)
    val nanoErgs: Long = boxes.map(_.value).sum
    val tokens: Map[ID, Amount] = boxes.flatMap(_.tokens).groupBy(_._1).map {
      case (k, v) => k -> v.map(_._2).sum
    }
    val ergs: String = nanoErgs / BigDecimal(1000000000) + " Ergs"
    val assets: Seq[String] = tokens.map { case (k, v) => k + " " + v }.toSeq
    ergs +: assets
  }

  private val randId = java.util.UUID.randomUUID().toString

  def send(toAddress: String, ergs: BigDecimal) = {
    val $INFO$ = "Using 0.001 Ergs as fee"
    val $ergs$ = "0.001"
    val nanoErgs = (ergs * BigDecimal(1000000000)).toBigInt().toLong
    val unspentBoxes: Seq[ergo.KioskBox] =
      explorer.getUnspentBoxes(myAddress).sortBy(-_.value)
    val boxName = randId
    $ergoBox.createBoxFromAddress(
      boxName,
      toAddress,
      Array(),
      Array(),
      Array(),
      nanoErgs,
      None
    )
    val inputs: Seq[String] = boxSelector(nanoErgs + defaultFee, unspentBoxes)
    val txJson = $ergoBox.createTx(
      inputBoxIds = inputs.toArray,
      dataInputBoxIds = Array(),
      outputBoxNames = Array(boxName),
      fee = defaultFee,
      changeAddress = myAddress,
      proveDlogSecrets = Array(secretKey.toString(10)),
      proveDhtDataNames = Array(),
      useContextVars = false,
      broadcast = true
    )
    $ergoBox.deleteBox(boxName)
    txJson
  }

  private val defaultFee = 1000000L

  private def boxSelector(
      totalNanoErgsNeeded: Long,
      unspentBoxes: Seq[ergo.KioskBox]
  ) = {
    var sum = 0L
    val unspentBoxSums: Seq[(Int, Long, Long)] = unspentBoxes.zipWithIndex.map {
      case (box, i) =>
        val sumBefore = sum
        sum = sumBefore + box.value
        (i + 1, sumBefore, sum)
    }
    val index: Int = unspentBoxSums
      .find {
        case (i, before, after) => before < totalNanoErgsNeeded && totalNanoErgsNeeded <= after
      }
      .getOrElse(
        throw new Exception(
          s"Insufficient funds. Short by ${totalNanoErgsNeeded - sum} nanoErgs"
        )
      )
      ._1
    unspentBoxes.take(index).map(_.optBoxId.get)
  }

  private val compiler = new TxBuilder(explorer)

  def txBuilder(jdeScript: Text, additionalSecrets: Array[String], broadcast: Boolean) = {

    val $INFO$ =
      """This creates a transaction using the script specified in TxBuilder. 
TxBuilder is built on top of JDE (https://github.com/ergoplatform/ergo-jde). 
If there are any lacking Ergs or tokens in the inputs, the wallet will attempt to add its own unspent boxes. 
The default script obtains the Erg-USD rate from the Oracle Pool.

If some of the inputs need additional (proveDlog) secrets, they should be added to Env (as BigInts) and referenced in additionalSecrets."""

    val $broadcast$ = "false"

    val $jdeScript$ =
      """{
  "constants": [
    {
      "name": "oraclePoolNFT",
      "type": "CollByte",
      "value": "011d3364de07e5a26f0c4eef0852cddb387039a921b7154ef3cab22c6eda887f"
    },
    {
      "name": "poolAddresses",
      "type": "Address",
      "values": [
        "NTkuk55NdwCXkF1e2nCABxq7bHjtinX3wH13zYPZ6qYT71dCoZBe1gZkh9FAr7GeHo2EpFoibzpNQmoi89atUjKRrhZEYrTapdtXrWU4kq319oY7BEWmtmRU9cMohX69XMuxJjJP5hRM8WQLfFnffbjshhEP3ck9CKVEkFRw1JDYkqVke2JVqoMED5yxLVkScbBUiJJLWq9BSbE1JJmmreNVskmWNxWE6V7ksKPxFMoqh1SVePh3UWAaBgGQRZ7TWf4dTBF5KMVHmRXzmQqEu2Fz2yeSLy23sM3pfqa78VuvoFHnTFXYFFxn3DNttxwq3EU3Zv25SmgrWjLKiZjFcEcqGgH6DJ9FZ1DfucVtTXwyDJutY3ksUBaEStRxoUQyRu4EhDobixL3PUWRcxaRJ8JKA9b64ALErGepRHkAoVmS8DaE6VbroskyMuhkTo7LbrzhTyJbqKurEzoEfhYxus7bMpLTePgKcktgRRyB7MjVxjSpxWzZedvzbjzZaHLZLkWZESk1WtdM25My33wtVLNXiTvficEUbjA23sNd24pv1YQ72nY1aqUHa2",
        "EfS5abyDe4vKFrJ48K5HnwTqa1ksn238bWFPe84bzVvCGvK1h2B7sgWLETtQuWwzVdBaoRZ1HcyzddrxLcsoM5YEy4UnqcLqMU1MDca1kLw9xbazAM6Awo9y6UVWTkQcS97mYkhkmx2Tewg3JntMgzfLWz5mACiEJEv7potayvk6awmLWS36sJMfXWgnEfNiqTyXNiPzt466cgot3GLcEsYXxKzLXyJ9EfvXpjzC2abTMzVSf1e17BHre4zZvDoAeTqr4igV3ubv2PtJjntvF2ibrDLmwwAyANEhw1yt8C8fCidkf3MAoPE6T53hX3Eb2mp3Xofmtrn4qVgmhNonnV8ekWZWvBTxYiNP8Vu5nc6RMDBv7P1c5rRc3tnDMRh2dUcDD7USyoB9YcvioMfAZGMNfLjWqgYu9Ygw2FokGBPThyWrKQ5nkLJvief1eQJg4wZXKdXWAR7VxwNftdZjPCHcmwn6ByRHZo9kb4Emv3rjfZE"
      ]
    }
  ],
  "auxInputs": [
    {
      "address": {
        "value": "poolAddresses"
      },
      "tokens": [
        { 
          "index": 0,
          "id": {
             "value": "oraclePoolNFT" 
          }
        }
      ],
      "registers": [
        {
          "num": "R4",
          "name": "rateUsd",
          "type": "Long"
        }
      ]
    }
  ],
  "returns": [
    "rateUsd"
  ]
}
"""

    val envMap = $ergoBox.$ergoScript.$env.$envMap
    val additionalBigIntSecrets = additionalSecrets.map { additionalSecret =>
      if (envMap.contains(additionalSecret)) {
        $ergoBox.$ergoScript.$env.$envMap(additionalSecret) match {
          case kioskBigInt: KioskBigInt => kioskBigInt.bigInt.toString(10)
          case any =>
            throw new Exception(
              s"$additionalSecret must be of type BigInt. Found ${any.typeName}"
            )
        }
      } else
        throw new Exception(
          s"Env does not contain (BigInt) variable $additionalSecret"
        )
    }

    val compileResults = compiler.compile(Parser.parse(jdeScript.getText))
    val txJson: JsValue = if (compileResults.outputs.nonEmpty) {
      try {
        val feeNanoErgs = compileResults.fee.getOrElse(defaultFee)
        val outputNanoErgs = compileResults.outputs.map(_.value).sum + feeNanoErgs
        val deficientNanoErgs =
          (outputNanoErgs - compileResults.inputNanoErgs).max(0)

        /* Currently we are not going to look for deficient tokens, just nanoErgs */
        val moreInputBoxIds = if (deficientNanoErgs > 0) {
          val myBoxes: Seq[ergo.KioskBox] = explorer
            .getUnspentBoxes(myAddress)
            .filterNot(compileResults.inputBoxIds.contains)
            .sortBy(-_.value)
          boxSelector(deficientNanoErgs, myBoxes)
        } else Nil
        val inputBoxIds = compileResults.inputBoxIds ++ moreInputBoxIds
        val txString = Client.usingContext { implicit ctx =>
          val inputBoxes: Array[InputBox] = ctx.getBoxesById(inputBoxIds: _*)
          val dataInputBoxes: Array[InputBox] =
            ctx.getBoxesById(compileResults.dataInputBoxIds: _*)
          TxUtil
            .createTx(
              inputBoxes = inputBoxes,
              dataInputs = dataInputBoxes,
              boxesToCreate = compileResults.outputs.toArray,
              fee = feeNanoErgs,
              changeAddress = myAddress,
              proveDlogSecrets =
                Array(secretKey.toString(10)) ++ additionalBigIntSecrets,
              dhtData = Array[DhtData](),
              broadcast = broadcast
            )
            .toJson(false)
        }
        Json.parse(txString)
      } catch {
        case throwable: Throwable => Json.obj("error" -> throwable.getMessage)
      }
      // only reading data
    } else Json.obj()
    import Parser._
    Json.obj("tx" -> txJson, "compiled" -> compileResults)
  }

  override def $setSession(sessionSecret: Option[String]): KioskWallet =
    new KioskWallet($ergoBox.$setSession(sessionSecret))
}
