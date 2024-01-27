package io.wispforest.uwu.text;

import io.wispforest.owo.serialization.Endec;
import io.wispforest.owo.serialization.endec.StructEndecBuilder;
import net.minecraft.network.chat.*;
import java.util.Optional;

public class BasedTextContent implements ComponentContents {

    public static final Type<BasedTextContent> TYPE = new Type<>(
            StructEndecBuilder.of(Endec.STRING.fieldOf("based", o -> o.basedText), BasedTextContent::new).mapCodec(),
            "uwu:based");

    private final String basedText;

    public BasedTextContent(String basedText) {
        this.basedText = basedText;
    }

    @Override
    public <T> Optional<T> visit(FormattedText.ContentConsumer<T> visitor) {
        return visitor.accept("I am extremely based: " + basedText);
    }

    @Override
    public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> visitor, Style style) {
        return visitor.accept(style, "I am extremely based: " + basedText);
    }

    @Override
    public Type<?> type() {
        return TYPE;
    }
}
