package com.pigicial.wikirenderer.render.skyblock.frame_based;

import com.pigicial.wikirenderer.property.PropertyBundle;
import com.pigicial.wikirenderer.render.Renderable;

public record FrameData<S, R extends Renderable<P>, P extends PropertyBundle>(int recordedTimingMsOffset, S sourceData, R renderable) {
}
