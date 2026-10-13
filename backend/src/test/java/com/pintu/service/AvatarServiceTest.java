package com.pintu.service;

import com.pintu.entity.User;
import com.pintu.mapper.UserMapper;
import com.pintu.service.storage.ObjectStorageProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mock.web.MockMultipartFile;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AvatarServiceTest {

    @Test
    void uploadCropsCompressesStoresAndUpdatesProfile() throws Exception {
        ObjectStorageProvider storage = mock(ObjectStorageProvider.class);
        UserMapper userMapper = mock(UserMapper.class);
        User user = new User();
        user.setId("passenger-test");
        when(userMapper.selectById(user.getId())).thenReturn(user);
        when(storage.put(anyString(), any(byte[].class), anyString()))
                .thenReturn("/api/files/avatars/passenger-test/avatar.jpg");

        BufferedImage source = new BufferedImage(900, 600, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = source.createGraphics();
        graphics.setColor(Color.BLUE);
        graphics.fillRect(0, 0, source.getWidth(), source.getHeight());
        graphics.dispose();
        ByteArrayOutputStream input = new ByteArrayOutputStream();
        ImageIO.write(source, "png", input);
        MockMultipartFile file = new MockMultipartFile(
                "file", "avatar.png", "image/png", input.toByteArray());

        String url = new AvatarService(storage, userMapper).upload(user.getId(), file);

        assertTrue(url.startsWith("/api/files/avatars/"));
        ArgumentCaptor<byte[]> bytes = ArgumentCaptor.forClass(byte[].class);
        verify(storage).put(anyString(), bytes.capture(), org.mockito.ArgumentMatchers.eq("image/jpeg"));
        BufferedImage stored = ImageIO.read(new ByteArrayInputStream(bytes.getValue()));
        assertEquals(512, stored.getWidth());
        assertEquals(512, stored.getHeight());
        assertEquals(url, user.getAvatarUrl());
        verify(userMapper).updateById(user);
    }
}
