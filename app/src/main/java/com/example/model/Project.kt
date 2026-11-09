package com.example.model

import com.example.model.effect.AppliedEffect
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

/**
 * Root Project structure containing composition settings and layered scene graph.
 */
data class Project(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Untitled Motion",
    val composition: Composition = Composition(),
    val layers: List<Layer> = emptyList()
) {
    fun toJsonString(indentSpaces: Int = 2): String {
        val root = JSONObject()
        root.put("id", id)
        root.put("name", name)

        val compObj = JSONObject().apply {
            put("width", composition.width)
            put("height", composition.height)
            put("fps", composition.fps)
            put("durationMs", composition.durationMs)
            put("backgroundColorHex", composition.backgroundColorHex)
        }
        root.put("composition", compObj)

        val layersArray = JSONArray()
        for (layer in layers) {
            val layerObj = JSONObject().apply {
                put("id", layer.id)
                put("name", layer.name)
                put("startTimeMs", layer.startTimeMs)
                put("durationMs", layer.durationMs)
                put("isVisible", layer.isVisible)
                put("isLocked", layer.isLocked)
                put("parentGroupId", layer.parentGroupId ?: JSONObject.NULL)

                // Type details
                val typeObj = JSONObject()
                when (val t = layer.type) {
                    is LayerType.Text -> {
                        typeObj.put("kind", "Text")
                        typeObj.put("text", t.text)
                        typeObj.put("fontSize", t.fontSize.toDouble())
                        typeObj.put("colorHex", t.colorHex)
                        typeObj.put("isBold", t.isBold)
                    }
                    is LayerType.ShapeRect -> {
                        typeObj.put("kind", "ShapeRect")
                        typeObj.put("width", t.width.toDouble())
                        typeObj.put("height", t.height.toDouble())
                        typeObj.put("fillColorHex", t.fillColorHex)
                        typeObj.put("cornerRadius", t.cornerRadius.toDouble())
                    }
                    is LayerType.ShapeCircle -> {
                        typeObj.put("kind", "ShapeCircle")
                        typeObj.put("radius", t.radius.toDouble())
                        typeObj.put("fillColorHex", t.fillColorHex)
                    }
                    is LayerType.SolidColor -> {
                        typeObj.put("kind", "SolidColor")
                        typeObj.put("fillColorHex", t.fillColorHex)
                        typeObj.put("width", t.width.toDouble())
                        typeObj.put("height", t.height.toDouble())
                    }
                    is LayerType.Media -> {
                        typeObj.put("kind", "Media")
                        typeObj.put("mediaType", t.mediaType)
                        typeObj.put("label", t.label)
                        typeObj.put("uri", t.uri ?: JSONObject.NULL)
                        val bgObj = JSONObject().apply {
                            put("isEnabled", t.bgRemoval.isEnabled)
                            put("edgeFeather", t.bgRemoval.edgeFeather.toDouble())
                            put("shrinkGrow", t.bgRemoval.shrinkGrow.toDouble())
                            put("previewMask", t.bgRemoval.previewMask)
                            put("threshold", t.bgRemoval.threshold.toDouble())
                        }
                        typeObj.put("bgRemoval", bgObj)
                    }
                    is LayerType.Adjustment -> {
                        typeObj.put("kind", "Adjustment")
                        typeObj.put("effectName", t.effectName)
                        typeObj.put("tintHex", t.tintHex)
                        typeObj.put("intensity", t.intensity.toDouble())
                    }
                    is LayerType.Group -> {
                        typeObj.put("kind", "Group")
                        val childrenArr = JSONArray()
                        t.childIds.forEach { childrenArr.put(it) }
                        typeObj.put("childIds", childrenArr)
                    }
                }
                put("type", typeObj)

                // Transform Tracks
                val transformObj = JSONObject()
                transformObj.put("scaleLinked", layer.transform.scaleLinked)
                transformObj.put("hasMotionBlur", layer.transform.hasMotionBlur)
                transformObj.put("positionX", trackToJson(layer.transform.positionX))
                transformObj.put("positionY", trackToJson(layer.transform.positionY))
                transformObj.put("scaleX", trackToJson(layer.transform.scaleX))
                transformObj.put("scaleY", trackToJson(layer.transform.scaleY))
                transformObj.put("pivotX", trackToJson(layer.transform.pivotX))
                transformObj.put("pivotY", trackToJson(layer.transform.pivotY))
                transformObj.put("rotation", trackToJson(layer.transform.rotation))
                transformObj.put("opacity", trackToJson(layer.transform.opacity))
                put("transform", transformObj)

                // Effects Stack
                val effectsArr = JSONArray()
                for (eff in layer.effects) {
                    val effObj = JSONObject()
                    effObj.put("id", eff.id)
                    effObj.put("effectDefId", eff.effectDefId)
                    effObj.put("isEnabled", eff.isEnabled)
                    val paramsObj = JSONObject()
                    for ((pId, track) in eff.parameterTracks) {
                        paramsObj.put(pId, trackToJson(track))
                    }
                    effObj.put("parameterTracks", paramsObj)
                    effectsArr.put(effObj)
                }
                put("effects", effectsArr)

                // Time Remap
                val trObj = JSONObject().apply {
                    put("isEnabled", layer.timeRemap.isEnabled)
                    put("speedMultiplier", layer.timeRemap.speedMultiplier.toDouble())
                    put("track", trackToJson(layer.timeRemap.track))
                }
                put("timeRemap", trObj)

                // Blend Mode & Track Matte
                put("blendMode", layer.blendMode.name)
                put("trackMatte", layer.trackMatte.name)
            }
            layersArray.put(layerObj)
        }
        root.put("layers", layersArray)

        return root.toString(indentSpaces)
    }

    companion object {
        private fun trackToJson(track: KeyframeTrack<Float>): JSONObject {
            val obj = JSONObject()
            obj.put("isAnimated", track.isAnimated)
            obj.put("defaultValue", track.defaultValue.toDouble())
            val kfArr = JSONArray()
            for (kf in track.keyframes) {
                val kfObj = JSONObject()
                kfObj.put("id", kf.id)
                kfObj.put("timeMs", kf.timeMs)
                kfObj.put("value", kf.value.toDouble())
                val curveObj = JSONObject().apply {
                    put("x1", kf.curveToNext.x1.toDouble())
                    put("y1", kf.curveToNext.y1.toDouble())
                    put("x2", kf.curveToNext.x2.toDouble())
                    put("y2", kf.curveToNext.y2.toDouble())
                }
                kfObj.put("curveToNext", curveObj)
                kfArr.put(kfObj)
            }
            obj.put("keyframes", kfArr)
            return obj
        }

        private fun trackFromJson(obj: JSONObject, defaultFallback: Float): KeyframeTrack<Float> {
            val isAnimated = obj.optBoolean("isAnimated", false)
            val defVal = obj.optDouble("defaultValue", defaultFallback.toDouble()).toFloat()
            val kfList = mutableListOf<Keyframe<Float>>()
            val kfArr = obj.optJSONArray("keyframes")
            if (kfArr != null) {
                for (i in 0 until kfArr.length()) {
                    val kfObj = kfArr.getJSONObject(i)
                    val curveObj = kfObj.optJSONObject("curveToNext")
                    val curve = if (curveObj != null) {
                        CubicBezier(
                            x1 = curveObj.optDouble("x1", 0.42).toFloat(),
                            y1 = curveObj.optDouble("y1", 0.0).toFloat(),
                            x2 = curveObj.optDouble("x2", 0.58).toFloat(),
                            y2 = curveObj.optDouble("y2", 1.0).toFloat()
                        )
                    } else {
                        CubicBezier.EaseInOut
                    }
                    kfList.add(
                        Keyframe(
                            id = kfObj.optString("id", UUID.randomUUID().toString()),
                            timeMs = kfObj.optLong("timeMs", 0L),
                            value = kfObj.optDouble("value", 0.0).toFloat(),
                            curveToNext = curve
                        )
                    )
                }
            }
            return KeyframeTrack(
                isAnimated = isAnimated,
                defaultValue = defVal,
                keyframes = kfList.sortedBy { it.timeMs }
            )
        }

        fun fromJsonString(jsonStr: String): Project {
            val root = JSONObject(jsonStr)
            val id = root.optString("id", UUID.randomUUID().toString())
            val name = root.optString("name", "Imported Project")

            val compObj = root.optJSONObject("composition")
            val composition = if (compObj != null) {
                Composition(
                    width = compObj.optInt("width", 1080),
                    height = compObj.optInt("height", 1920),
                    fps = compObj.optInt("fps", 30),
                    durationMs = compObj.optLong("durationMs", 5000L),
                    backgroundColorHex = compObj.optString("backgroundColorHex", "#0A0B10")
                )
            } else {
                Composition()
            }

            val layersList = mutableListOf<Layer>()
            val layersArr = root.optJSONArray("layers")
            if (layersArr != null) {
                for (i in 0 until layersArr.length()) {
                    val lObj = layersArr.getJSONObject(i)
                    val layerId = lObj.optString("id", UUID.randomUUID().toString())
                    val layerName = lObj.optString("name", "Layer $i")
                    val startMs = lObj.optLong("startTimeMs", 0L)
                    val durMs = lObj.optLong("durationMs", composition.durationMs)
                    val visible = lObj.optBoolean("isVisible", true)
                    val locked = lObj.optBoolean("isLocked", false)
                    val parentGroup = if (lObj.isNull("parentGroupId")) null else lObj.optString("parentGroupId")

                    // Type
                    val typeObj = lObj.optJSONObject("type")
                    val layerType: LayerType = when (typeObj?.optString("kind")) {
                        "Text" -> LayerType.Text(
                            text = typeObj.optString("text", "Text"),
                            fontSize = typeObj.optDouble("fontSize", 72.0).toFloat(),
                            colorHex = typeObj.optString("colorHex", "#FFFFFF"),
                            isBold = typeObj.optBoolean("isBold", true)
                        )
                        "ShapeRect" -> LayerType.ShapeRect(
                            width = typeObj.optDouble("width", 360.0).toFloat(),
                            height = typeObj.optDouble("height", 240.0).toFloat(),
                            fillColorHex = typeObj.optString("fillColorHex", "#00E5FF"),
                            cornerRadius = typeObj.optDouble("cornerRadius", 24.0).toFloat()
                        )
                        "ShapeCircle" -> LayerType.ShapeCircle(
                            radius = typeObj.optDouble("radius", 160.0).toFloat(),
                            fillColorHex = typeObj.optString("fillColorHex", "#8B5CF6")
                        )
                        "SolidColor" -> LayerType.SolidColor(
                            fillColorHex = typeObj.optString("fillColorHex", "#1B1F33"),
                            width = typeObj.optDouble("width", 1080.0).toFloat(),
                            height = typeObj.optDouble("height", 1920.0).toFloat()
                        )
                        "Media" -> {
                            val bgObj = typeObj.optJSONObject("bgRemoval")
                            val bgConfig = if (bgObj != null) {
                                BackgroundRemovalConfig(
                                    isEnabled = bgObj.optBoolean("isEnabled", false),
                                    edgeFeather = bgObj.optDouble("edgeFeather", 6.0).toFloat(),
                                    shrinkGrow = bgObj.optDouble("shrinkGrow", 0.0).toFloat(),
                                    previewMask = bgObj.optBoolean("previewMask", false),
                                    threshold = bgObj.optDouble("threshold", 0.5).toFloat()
                                )
                            } else BackgroundRemovalConfig()

                            LayerType.Media(
                                mediaType = typeObj.optString("mediaType", "image"),
                                label = typeObj.optString("label", "Media"),
                                uri = if (typeObj.isNull("uri")) null else typeObj.optString("uri"),
                                bgRemoval = bgConfig
                            )
                        }
                        "Adjustment" -> LayerType.Adjustment(
                            effectName = typeObj.optString("effectName", "Color Tint"),
                            tintHex = typeObj.optString("tintHex", "#7C3AED"),
                            intensity = typeObj.optDouble("intensity", 0.5).toFloat()
                        )
                        "Group" -> {
                            val cList = mutableListOf<String>()
                            val cArr = typeObj.optJSONArray("childIds")
                            if (cArr != null) {
                                for (ci in 0 until cArr.length()) cList.add(cArr.getString(ci))
                            }
                            LayerType.Group(cList)
                        }
                        else -> LayerType.Text()
                    }

                    // Transform
                    val tObj = lObj.optJSONObject("transform")
                    val scaleLinked = tObj?.optBoolean("scaleLinked", true) ?: true
                    val transformTracks = LayerTransformTracks(
                        positionX = tObj?.optJSONObject("positionX")?.let { trackFromJson(it, 540f) } ?: KeyframeTrack(defaultValue = 540f),
                        positionY = tObj?.optJSONObject("positionY")?.let { trackFromJson(it, 960f) } ?: KeyframeTrack(defaultValue = 960f),
                        scaleX = tObj?.optJSONObject("scaleX")?.let { trackFromJson(it, 100f) } ?: KeyframeTrack(defaultValue = 100f),
                        scaleY = tObj?.optJSONObject("scaleY")?.let { trackFromJson(it, 100f) } ?: KeyframeTrack(defaultValue = 100f),
                        scaleLinked = scaleLinked,
                        pivotX = tObj?.optJSONObject("pivotX")?.let { trackFromJson(it, 0.5f) } ?: KeyframeTrack(defaultValue = 0.5f),
                        pivotY = tObj?.optJSONObject("pivotY")?.let { trackFromJson(it, 0.5f) } ?: KeyframeTrack(defaultValue = 0.5f),
                        rotation = tObj?.optJSONObject("rotation")?.let { trackFromJson(it, 0f) } ?: KeyframeTrack(defaultValue = 0f),
                        opacity = tObj?.optJSONObject("opacity")?.let { trackFromJson(it, 100f) } ?: KeyframeTrack(defaultValue = 100f),
                        hasMotionBlur = tObj?.optBoolean("hasMotionBlur", false) ?: false
                    )

                    val effectsList = mutableListOf<com.example.model.effect.AppliedEffect>()
                    val effArr = lObj.optJSONArray("effects")
                    if (effArr != null) {
                        for (ei in 0 until effArr.length()) {
                            val effObj = effArr.getJSONObject(ei)
                            val effId = effObj.optString("id", UUID.randomUUID().toString())
                            val defId = effObj.optString("effectDefId", "")
                            val isEnabled = effObj.optBoolean("isEnabled", true)
                            val pTracks = mutableMapOf<String, KeyframeTrack<Float>>()
                            val pObj = effObj.optJSONObject("parameterTracks")
                            if (pObj != null) {
                                val keys = pObj.keys()
                                while (keys.hasNext()) {
                                    val k = keys.next()
                                    val tJson = pObj.getJSONObject(k)
                                    pTracks[k] = trackFromJson(tJson, 0f)
                                }
                            }
                            effectsList.add(
                                com.example.model.effect.AppliedEffect(
                                    id = effId,
                                    effectDefId = defId,
                                    isEnabled = isEnabled,
                                    parameterTracks = pTracks
                                )
                            )
                        }
                    }

                    val trObj = lObj.optJSONObject("timeRemap")
                    val timeRemap = if (trObj != null) {
                        TimeRemapConfig(
                            isEnabled = trObj.optBoolean("isEnabled", false),
                            speedMultiplier = trObj.optDouble("speedMultiplier", 1.0).toFloat(),
                            track = trObj.optJSONObject("track")?.let { trackFromJson(it, 0f) } ?: KeyframeTrack(defaultValue = 0f)
                        )
                    } else TimeRemapConfig()

                    val blendModeStr = lObj.optString("blendMode", "NORMAL")
                    val blendMode = try {
                        LayerBlendMode.valueOf(blendModeStr)
                    } catch (e: Exception) {
                        LayerBlendMode.NORMAL
                    }

                    val trackMatteStr = lObj.optString("trackMatte", "NONE")
                    val trackMatte = try {
                        TrackMatteType.valueOf(trackMatteStr)
                    } catch (e: Exception) {
                        TrackMatteType.NONE
                    }

                    layersList.add(
                        Layer(
                            id = layerId,
                            name = layerName,
                            type = layerType,
                            startTimeMs = startMs,
                            durationMs = durMs,
                            isVisible = visible,
                            isLocked = locked,
                            transform = transformTracks,
                            parentGroupId = parentGroup,
                            effects = effectsList,
                            timeRemap = timeRemap,
                            blendMode = blendMode,
                            trackMatte = trackMatte
                        )
                    )
                }
            }

            return Project(
                id = id,
                name = name,
                composition = composition,
                layers = layersList
            )
        }

        fun createSampleProject(): Project {
            val comp = Composition(
                width = 1080,
                height = 1920,
                fps = 30,
                durationMs = 5000L,
                backgroundColorHex = "#0C0D14"
            )

            // 1. Solid backdrop layer
            val solidLayer = Layer(
                name = "Deep Obsidian BG",
                type = LayerType.SolidColor(fillColorHex = "#0F111D"),
                startTimeMs = 0L,
                durationMs = 5000L,
                transform = LayerTransformTracks(
                    positionX = KeyframeTrack(defaultValue = 540f),
                    positionY = KeyframeTrack(defaultValue = 960f),
                    scaleX = KeyframeTrack(defaultValue = 100f),
                    scaleY = KeyframeTrack(defaultValue = 100f),
                    opacity = KeyframeTrack(defaultValue = 100f)
                )
            )

            // 2. Animated Cyan Rect Shape (Rotates and scales with keyframes)
            val rectLayer = Layer(
                name = "Cyan Portal Plate",
                type = LayerType.ShapeRect(
                    width = 440f,
                    height = 440f,
                    fillColorHex = "#00E5FF",
                    cornerRadius = 48f
                ),
                startTimeMs = 0L,
                durationMs = 5000L,
                transform = LayerTransformTracks(
                    positionX = KeyframeTrack(
                        isAnimated = true,
                        defaultValue = 540f,
                        keyframes = listOf(
                            Keyframe(timeMs = 0L, value = 400f),
                            Keyframe(timeMs = 2500L, value = 540f),
                            Keyframe(timeMs = 5000L, value = 680f)
                        )
                    ),
                    positionY = KeyframeTrack(
                        isAnimated = true,
                        defaultValue = 820f,
                        keyframes = listOf(
                            Keyframe(timeMs = 0L, value = 900f),
                            Keyframe(timeMs = 2500L, value = 780f),
                            Keyframe(timeMs = 5000L, value = 900f)
                        )
                    ),
                    rotation = KeyframeTrack(
                        isAnimated = true,
                        defaultValue = 0f,
                        keyframes = listOf(
                            Keyframe(timeMs = 0L, value = 0f),
                            Keyframe(timeMs = 2500L, value = 90f),
                            Keyframe(timeMs = 5000L, value = 180f)
                        )
                    ),
                    scaleX = KeyframeTrack(
                        isAnimated = true,
                        defaultValue = 100f,
                        keyframes = listOf(
                            Keyframe(timeMs = 0L, value = 80f),
                            Keyframe(timeMs = 2500L, value = 120f),
                            Keyframe(timeMs = 5000L, value = 80f)
                        )
                    ),
                    scaleY = KeyframeTrack(
                        isAnimated = true,
                        defaultValue = 100f,
                        keyframes = listOf(
                            Keyframe(timeMs = 0L, value = 80f),
                            Keyframe(timeMs = 2500L, value = 120f),
                            Keyframe(timeMs = 5000L, value = 80f)
                        )
                    ),
                    opacity = KeyframeTrack(defaultValue = 85f)
                )
            )

            // 3. Purple Circle Shape
            val circleLayer = Layer(
                name = "Violet Pulse Core",
                type = LayerType.ShapeCircle(
                    radius = 180f,
                    fillColorHex = "#8B5CF6"
                ),
                startTimeMs = 500L,
                durationMs = 4500L,
                transform = LayerTransformTracks(
                    positionX = KeyframeTrack(defaultValue = 540f),
                    positionY = KeyframeTrack(defaultValue = 820f),
                    scaleX = KeyframeTrack(
                        isAnimated = true,
                        defaultValue = 100f,
                        keyframes = listOf(
                            Keyframe(timeMs = 500L, value = 50f),
                            Keyframe(timeMs = 2500L, value = 110f),
                            Keyframe(timeMs = 5000L, value = 70f)
                        )
                    ),
                    scaleY = KeyframeTrack(
                        isAnimated = true,
                        defaultValue = 100f,
                        keyframes = listOf(
                            Keyframe(timeMs = 500L, value = 50f),
                            Keyframe(timeMs = 2500L, value = 110f),
                            Keyframe(timeMs = 5000L, value = 70f)
                        )
                    ),
                    opacity = KeyframeTrack(defaultValue = 90f)
                )
            )

            // 4. Hero Text Layer
            val textLayer = Layer(
                name = "Title Text",
                type = LayerType.Text(
                    text = "LUMALAYER",
                    fontSize = 84f,
                    colorHex = "#FFFFFF",
                    isBold = true
                ),
                startTimeMs = 0L,
                durationMs = 5000L,
                transform = LayerTransformTracks(
                    positionX = KeyframeTrack(defaultValue = 540f),
                    positionY = KeyframeTrack(
                        isAnimated = true,
                        defaultValue = 1280f,
                        keyframes = listOf(
                            Keyframe(timeMs = 0L, value = 1400f),
                            Keyframe(timeMs = 1500L, value = 1280f)
                        )
                    ),
                    scaleX = KeyframeTrack(defaultValue = 100f),
                    scaleY = KeyframeTrack(defaultValue = 100f),
                    opacity = KeyframeTrack(
                        isAnimated = true,
                        defaultValue = 100f,
                        keyframes = listOf(
                            Keyframe(timeMs = 0L, value = 0f),
                            Keyframe(timeMs = 1000L, value = 100f)
                        )
                    )
                )
            )

            // 5. Media Layer with Background Removal (Phase 4)
            val mediaLayer = Layer(
                name = "Portrait Subject",
                type = LayerType.Media(
                    mediaType = "image",
                    label = "Neon Character",
                    bgRemoval = BackgroundRemovalConfig(
                        isEnabled = false,
                        edgeFeather = 8f,
                        shrinkGrow = 0f,
                        previewMask = false,
                        threshold = 0.5f
                    )
                ),
                startTimeMs = 0L,
                durationMs = 5000L,
                transform = LayerTransformTracks(
                    positionX = KeyframeTrack(defaultValue = 540f),
                    positionY = KeyframeTrack(defaultValue = 820f),
                    scaleX = KeyframeTrack(defaultValue = 110f),
                    scaleY = KeyframeTrack(defaultValue = 110f),
                    opacity = KeyframeTrack(defaultValue = 100f)
                ),
                effects = listOf(
                    AppliedEffect(
                        effectDefId = "wave_warp",
                        isEnabled = true,
                        parameterTracks = mapOf(
                            "wave_height" to KeyframeTrack(
                                isAnimated = true,
                                defaultValue = 12f,
                                keyframes = listOf(
                                    Keyframe(timeMs = 0L, value = 4f),
                                    Keyframe(timeMs = 2500L, value = 18f),
                                    Keyframe(timeMs = 5000L, value = 4f)
                                )
                            ),
                            "wave_width" to KeyframeTrack(defaultValue = 70f),
                            "direction" to KeyframeTrack(defaultValue = 0f),
                            "speed" to KeyframeTrack(defaultValue = 1.5f)
                        )
                    )
                )
            )

            return Project(
                id = UUID.randomUUID().toString(),
                name = "Cyberpunk Intro",
                composition = comp,
                layers = listOf(solidLayer, mediaLayer, rectLayer, circleLayer, textLayer)
            )
        }
    }
}
