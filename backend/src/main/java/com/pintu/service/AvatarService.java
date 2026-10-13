package com.pintu.service;

import com.pintu.common.exception.BusinessException;
import com.pintu.entity.User;
import com.pintu.mapper.UserMapper;
import com.pintu.service.storage.ObjectStorageProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AvatarService {

    private static final long MAX_BYTES = 5 * 1024 * 1024;
    private static final int OUTPUT_SIZE = 512;
    private static final Set<String> TYPES = Set.of("image/jpeg", "image/png");

    private final ObjectStorageProvider storageProvider;
    private final UserMapper userMapper;

    @Transactional(rollbackFor = Exception.class)
    public String upload(String userId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(1002, "Avatar file is required");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException(1002, "Avatar must not exceed 5 MB");
        }
        if (!TYPES.contains(file.getContentType())) {
            throw new BusinessException(1002, "Only JPEG and PNG avatars are supported");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(1404, "User does not exist");
        }
        byte[] normalized = normalize(file);
        String key = "avatars/" + userId + "/" + UUID.randomUUID() + ".jpg";
        String url = storageProvider.put(key, normalized, "image/jpeg");
        user.setAvatarUrl(url);
        userMapper.updateById(user);
        return url;
    }

    private byte[] normalize(MultipartFile file) {
        try {
            BufferedImage source = ImageIO.read(new ByteArrayInputStream(file.getBytes()));
            if (source == null) {
                throw new BusinessException(1002, "Invalid image content");
            }
            int side = Math.min(source.getWidth(), source.getHeight());
            int x = (source.getWidth() - side) / 2;
            int y = (source.getHeight() - side) / 2;
            BufferedImage output = new BufferedImage(
                    OUTPUT_SIZE, OUTPUT_SIZE, BufferedImage.TYPE_INT_RGB);
            Graphics2D graphics = output.createGraphics();
            graphics.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.drawImage(source, 0, 0, OUTPUT_SIZE, OUTPUT_SIZE,
                    x, y, x + side, y + side, null);
            graphics.dispose();
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ImageIO.write(output, "jpg", bytes);
            return bytes.toByteArray();
        } catch (IOException ex) {
            throw new BusinessException(1002, "Unable to process avatar image");
        }
    }
}

