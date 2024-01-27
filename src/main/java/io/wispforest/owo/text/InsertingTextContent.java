package io.wispforest.owo.text;

import io.wispforest.owo.serialization.Endec;
import io.wispforest.owo.serialization.endec.StructEndecBuilder;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentContents;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;

public record InsertingTextContent(int index) implements ComponentContents {

    public static final ComponentContents.Type<InsertingTextContent> TYPE = new Type<>(
            StructEndecBuilder.of(Endec.INT.fieldOf("index", InsertingTextContent::index), InsertingTextContent::new).mapCodec(),
            "owo:insert"
    );

    @Override
    public <T> Optional<T> visit(FormattedText.ContentConsumer<T> visitor) {
        var current = TranslationContext.getCurrent();

        if (current == null || current.getArgs().length <= index) {return visitor.accept("%" + (index + 1) + "$s");}

        Object arg = current.getArgs()[index];

        if (arg instanceof Component text) {
            return text.visit(visitor);
        } else {
            return visitor.accept(arg.toString());
        }
    }

    @Override
    public <T> Optional<T> visit(FormattedText.StyledContentConsumer<T> visitor, Style style) {
        var current = TranslationContext.getCurrent();

        if (current == null || current.getArgs().length <= index) {
            return visitor.accept(style, "%" + (index + 1) + "$s");
        }

        Object arg = current.getArgs()[index];

        if (arg instanceof Component text) {
            return text.visit(visitor, style);
        } else {
            return visitor.accept(style, arg.toString());
        }
    }

    @Override
    public Type<?> type() {
        return TYPE;
    }
}
