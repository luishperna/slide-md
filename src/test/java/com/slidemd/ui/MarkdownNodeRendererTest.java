package com.slidemd.ui;

import com.slidemd.model.Alignment;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import org.commonmark.parser.Parser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MarkdownNodeRendererTest {

    @BeforeAll
    static void initJfx() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {}
    }

    @Test
    void testHeadingColorsLightenWithLevelWhenCustomColorSpecified() {
        Parser parser = Parser.builder().build();
        MarkdownNodeRenderer renderer = new MarkdownNodeRenderer(new File("."), Alignment.CENTER, "#ff0000");

        String md = """
                # Titulo 1
                ## Titulo 2
                ### Titulo 3
                """;

        org.commonmark.node.Node document = parser.parse(md);
        List<Node> nodes = renderer.renderNodes(document);

        assertEquals(3, nodes.size());
        Label h1 = (Label) nodes.get(0);
        Label h2 = (Label) nodes.get(1);
        Label h3 = (Label) nodes.get(2);

        // H1 é vermelho puro (#ff0000)
        assertTrue(h1.getStyle().contains("#ff0000"), "H1 deve ter a cor vermelha pura: " + h1.getStyle());

        // H2 e H3 vão clareando (aumentando os componentes verde e azul ao misturar com branco)
        // Por exemplo #ff3333 e #ff6666
        assertNotEquals(h1.getStyle(), h2.getStyle(), "H2 deve ter cor diferente de H1");
        assertNotEquals(h2.getStyle(), h3.getStyle(), "H3 deve ter cor mais clara que H2");
    }

    @Test
    void testBulletColorMatchesPrimaryColor() {
        Parser parser = Parser.builder().build();
        MarkdownNodeRenderer renderer = new MarkdownNodeRenderer(new File("."), Alignment.CENTER, "#ff0000");

        String md = """
                - Item da lista
                """;

        org.commonmark.node.Node document = parser.parse(md);
        List<Node> nodes = renderer.renderNodes(document);

        assertEquals(1, nodes.size());
        assertTrue(nodes.get(0) instanceof VBox);
        VBox vbox = (VBox) nodes.get(0);
        HBox row = (HBox) vbox.getChildren().get(0);
        Label bullet = (Label) row.getChildren().get(0);

        assertEquals("•", bullet.getText());
        assertTrue(bullet.getStyle().contains("#ff0000"), "Bullet deve ter a cor vermelha: " + bullet.getStyle());
    }
}
