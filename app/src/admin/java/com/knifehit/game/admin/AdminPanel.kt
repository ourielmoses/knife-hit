package com.knifehit.game.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.knifehit.game.i18n.LocalStrings
import com.knifehit.game.ui.NeonButton
import com.knifehit.game.ui.NeonMagenta
import com.knifehit.game.ui.OverlayScrim
import com.knifehit.game.ui.Panel

@Composable
fun AdminPanel(onClose: () -> Unit, host: AdminHost) {
    val s = LocalStrings.current
    var pass by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var json by remember { mutableStateOf(host.exportJson()) }
    OverlayScrim(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(8.dp)) {
            NeonButton(s.back, onClose)
            Spacer(Modifier.height(8.dp))
            Text(s.developerPanel, color = Color.White)
            if (!host.unlocked) {
                Spacer(Modifier.height(12.dp))
                Panel(Modifier.fillMaxWidth()) {
                    Column {
                        OutlinedTextField(pass, { pass = it }, label = { Text(s.passcode) })
                        if (host.cooldown) Text("Cooldown…", color = NeonMagenta)
                        if (error != null) Text(error!!, color = NeonMagenta)
                        NeonButton(s.unlock, {
                            error = if (host.onUnlock(pass)) null else s.wrongPasscode
                        }, enabled = !host.cooldown)
                    }
                }
                return@Column
            }
            Spacer(Modifier.height(8.dp))
            Panel {
                Column {
                    Text(s.knifeGenerator, color = Color.White)
                    var id by remember { mutableStateOf("") }
                    var name by remember { mutableStateOf("") }
                    var color by remember { mutableStateOf("#39FF14") }
                    var speed by remember { mutableStateOf("1.1") }
                    var coins by remember { mutableStateOf("100") }
                    var diamonds by remember { mutableStateOf("0") }
                    var timbre by remember { mutableStateOf("BLADE") }
                    OutlinedTextField(id, { id = it }, label = { Text("id") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(name, { name = it }, label = { Text("name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(color, { color = it }, label = { Text("color hex") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(speed, { speed = it }, label = { Text("throwSpeed") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(coins, { coins = it }, label = { Text("priceInCoins (0 = not sold)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(diamonds, { diamonds = it }, label = { Text("priceInDiamonds (0 = not sold)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(timbre, { timbre = it }, label = { Text("timbre HEAVY/BLADE/LASER/ARCANE") }, modifier = Modifier.fillMaxWidth())
                    var kErr by remember { mutableStateOf<String?>(null) }
                    if (kErr != null) Text(kErr!!, color = NeonMagenta)
                    NeonButton(s.addKnife, {
                        kErr = host.addSkin(id, name, color, speed, coins, diamonds, timbre)
                    })
                }
            }
            Spacer(Modifier.height(8.dp))
            Panel {
                Column {
                    Text(s.worldGenerator, color = Color.White)
                    var wname by remember { mutableStateOf("") }
                    var top by remember { mutableStateOf("#050308") }
                    var bot by remember { mutableStateOf("#160C24") }
                    OutlinedTextField(wname, { wname = it }, label = { Text("name") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(top, { top = it }, label = { Text("bg top hex") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(bot, { bot = it }, label = { Text("bg bottom hex") }, modifier = Modifier.fillMaxWidth())
                    var wErr by remember { mutableStateOf<String?>(null) }
                    if (wErr != null) Text(wErr!!, color = NeonMagenta)
                    NeonButton(s.addWorld, { wErr = host.addWorld(wname, top, bot) })
                }
            }
            Spacer(Modifier.height(8.dp))
            Panel {
                Column {
                    Text(s.prices, color = Color.White)
                    val rows = remember(host.priceRows) {
                        host.priceRows.map { Triple(it.first, it.second.toString(), it.third.toString()) }.toMutableList()
                    }
                    host.priceRows.forEachIndexed { i, row ->
                        var c by remember(row) { mutableStateOf(row.second.toString()) }
                        var d by remember(row) { mutableStateOf(row.third.toString()) }
                        Text(row.first, color = Color.White)
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(c, { c = it }, label = { Text("coins") }, modifier = Modifier.weight(1f))
                            OutlinedTextField(d, { d = it }, label = { Text("diamonds") }, modifier = Modifier.weight(1f))
                        }
                        rows[i] = Triple(row.first, c, d)
                        // store via side map
                    }
                    var pErr by remember { mutableStateOf<String?>(null) }
                    if (pErr != null) Text(pErr!!, color = NeonMagenta)
                    NeonButton(s.commit, {
                        val parsed = host.priceRows.map { Triple(it.first, it.second, it.third) }
                        pErr = host.commitPrices(parsed)
                    })
                    NeonButton(s.resetDefaults, host.reset, accent = NeonMagenta)
                }
            }
            Spacer(Modifier.height(8.dp))
            Panel {
                Column {
                    NeonButton("Mint 500 rings", { host.mint(500, 0) })
                    NeonButton("Mint 20 diamonds", { host.mint(0, 20) })
                    NeonButton("Unlock all knives", host.unlockAll)
                }
            }
            Spacer(Modifier.height(8.dp))
            Panel {
                Column {
                    Text(s.exportJson, color = Color.White)
                    OutlinedTextField(json, { json = it }, modifier = Modifier.fillMaxWidth().height(180.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NeonButton(s.exportJson, { json = host.exportJson() })
                        NeonButton(s.importJson, { host.importJson(json) })
                    }
                }
            }
        }
    }
}
