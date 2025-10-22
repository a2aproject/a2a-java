package io.a2a.server.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import io.a2a.spec.Artifact;
import io.a2a.spec.DataPart;
import io.a2a.spec.Part;
import io.a2a.spec.TextPart;

import static org.mockito.Mockito.mockStatic;
class ArtifactUtilsTest {

    @Test
    void testNewArtifactGeneratesId() {
        // Given
        UUID mockUuid = UUID.fromString("abcdef12-1234-5678-1234-567812345678");
        
        try (MockedStatic<UUID> mockedUuid = mockStatic(UUID.class)) {
            mockedUuid.when(UUID::randomUUID).thenReturn(mockUuid);
            
            // When
            Artifact artifact = ArtifactUtils.newArtifact(
            List.of(new TextPart("")),
            "test_artifact"
        );
            
            // Then
            assertEquals(mockUuid.toString(), artifact.artifactId());
        }
    }

    @Test
    void testNewArtifactAssignsPartsNameDescription() {
        // Given
        List<Part<?>> parts = List.of(new TextPart("Sample text"));
        String name = "My Artifact";
        String description = "This is a test artifact.";
        
        // When
        Artifact artifact = ArtifactUtils.newArtifact(parts, name, description);
        
        // Then
        assertEquals(parts, artifact.parts());
        assertEquals(name, artifact.name());
        assertEquals(description, artifact.description());
    }

    @Test
    void testNewArtifactEmptyDescriptionIfNotProvided() {
        // Given
        List<Part<?>> parts = List.of(new TextPart("Another sample"));
        String name = "Artifact_No_Desc";
        
        // When
        Artifact artifact = ArtifactUtils.newArtifact(parts, name);
        
        // Then
        assertNull(artifact.description());
    }

    @Test
    void testNewTextArtifactCreatesSingleTextPart() {
        // Given
        String text = "This is a text artifact.";
        String name = "Text_Artifact";
        
        // When
        Artifact artifact = ArtifactUtils.newTextArtifact(name, text);
        
        // Then
        assertEquals(1, artifact.parts().size());
        assertInstanceOf(TextPart.class, artifact.parts().get(0));
    }

    @Test
    void testNewTextArtifactPartContainsProvidedText() {
        // Given
        String text = "Hello, world!";
        String name = "Greeting_Artifact";
        
        // When
        Artifact artifact = ArtifactUtils.newTextArtifact(name, text);
        
        // Then
        TextPart textPart = (TextPart) artifact.parts().get(0);
        assertEquals(text, textPart.getText());
    }

    @Test
    void testNewTextArtifactAssignsNameDescription() {
        // Given
        String text = "Some content.";
        String name = "Named_Text_Artifact";
        String description = "Description for text artifact.";
        
        // When
        Artifact artifact = ArtifactUtils.newTextArtifact(name, text, description);
        
        // Then
        assertEquals(name, artifact.name());
        assertEquals(description, artifact.description());
    }

    @Test
    void testNewDataArtifactCreatesSingleDataPart() {
        // Given
        Map<String, Object> sampleData = Map.of("key", "value", "number", 123);
        String name = "Data_Artifact";
        
        // When
        Artifact artifact = ArtifactUtils.newDataArtifact(name, sampleData);
        
        // Then
        assertEquals(1, artifact.parts().size());
        assertInstanceOf(DataPart.class, artifact.parts().get(0));
    }

    @Test
    void testNewDataArtifactPartContainsProvidedData() {
        // Given
        Map<String, Object> sampleData = Map.of("content", "test_data", "is_valid", true);
        String name = "Structured_Data_Artifact";
        
        // When
        Artifact artifact = ArtifactUtils.newDataArtifact(name, sampleData);
        
        // Then
        DataPart dataPart = (DataPart) artifact.parts().get(0);
        assertEquals(sampleData, dataPart.getData());
    }

    @Test
    void testNewDataArtifactAssignsNameDescription() {
        // Given
        Map<String, Object> sampleData = Map.of("info", "some details");
        String name = "Named_Data_Artifact";
        String description = "Description for data artifact.";
        
        // When
        Artifact artifact = ArtifactUtils.newDataArtifact(name, sampleData, description);
        
        // Then
        assertEquals(name, artifact.name());
        assertEquals(description, artifact.description());
    }

    @Test
    void testArtifactIdIsNotNull() {
        // Given
        List<Part<?>> parts = List.of(new TextPart("Test"));
        String name = "Test_Artifact";
        
        // When
        Artifact artifact = ArtifactUtils.newArtifact(parts, name);
        
        // Then
        assertNotNull(artifact.artifactId());
        assertTrue(artifact.artifactId().length() > 0);
    }
}
