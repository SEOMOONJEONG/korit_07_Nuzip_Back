package com.highlight.nuzip.service;

import com.highlight.nuzip.model.NewsArticle;
import com.highlight.nuzip.model.NewsCategory;
import com.highlight.nuzip.model.User;
import com.highlight.nuzip.repository.NewsArticleRepository;
import com.highlight.nuzip.repository.UserRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

/*
 * 매일 오전 9시(Asia/Seoul) 기준으로 가입된 모든 사용자에게
 * userId(이메일)로 공지 메일을 발송하는 스케줄러입니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DailyBroadcastEmailScheduler {

    private final UserRepository userRepository;
    private final NewsArticleRepository newsArticleRepository;
    private final GmailOAuthMailSender mailSender;

    @Value("${daily.mail.subject:[NUZIP] Daily Notice}")
    private String subject;

    @Value("${daily.mail.body:안녕하세요, Nuzip 입니다.}")
    private String body;

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void sendDailyBroadcast() {
        log.info("09:00 메일을 발송합니다.");

        List<User> users = userRepository.findAll();
        if (users.isEmpty()) {
            log.info("발송 대상 사용자가 없습니다.");
            log.info("메일 발송을 완료하였습니다.");
            return;
        }

        for (User user : users) {
            String email = user.getUserId();
            if (!StringUtils.hasText(email)) {
                log.warn("이메일 정보가 없어 발송을 건너뜁니다. userPk={}", user.getId());
                continue;
            }
            try {
                String personalizedBody = buildEmailBodyForUser(user);
                mailSender.send(email, subject, personalizedBody);
            } catch (Exception e) {
                log.error("사용자 {} ({}) 메일 발송 실패", user.getId(), email, e);
            }
        }

        log.info("메일 발송을 완료하였습니다.");
    }

    private String buildEmailBodyForUser(User user) {

        String logoUrl = "https://raw.githubusercontent.com/SEOMOONJEONG/korit_07_Nuzip_Front/main/src/pages/Nuzip_logo2.png";

        Set<NewsCategory> rawCategories = user.getNewsCategory();
        List<NewsCategory> categories = (CollectionUtils.isEmpty(rawCategories) ? List.<NewsCategory>of() :
                rawCategories.stream()
                        .filter(java.util.Objects::nonNull)
                        .sorted(Comparator.comparingInt(Enum::ordinal))
                        .limit(3)
                        .toList());

        StringBuilder sb = new StringBuilder();
        sb.append("""
        <div style="font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 30px;">
          <div style="max-width: 600px; margin: 0 auto; background-color: #f1f3f5; border-radius: 8px; overflow: hidden;">
    
            <!-- 배너 (로고 적용 + 인증메일과 동일 파란 테마) -->
            <div style="
                background-color: #DCEBFF;
                padding: 28px 20px;
                text-align: center;
            ">
              <img
                src=\"%s\"
                alt=\"Nuzip Logo\"
                style=\"height: 52px; width: auto; display: inline-block;\">
            </div>
    
            <!-- 본문 -->
            <div style="padding: 30px 24px; color: #111111; text-align: left; background-color: #ffffff;">
    
              <h2 style="margin-top: 6px; font-size: 20px; font-weight: bold;">
                오늘의 뉴스 하이라이트
              </h2>
    
              <hr style="margin: 18px 0 20px 0; border: 0; border-top: 1px solid #d0d4d8;" />
        """.formatted(logoUrl));


        // 카테고리 없을 때 메시지
        if (categories.isEmpty()) {
            sb.append("""
            <p style="font-size: 14px; color: #111111; line-height: 1.6;">
              아직 관심 카테고리가 설정되어 있지 않아, 오늘은 공지 메시지만 전해드립니다.<br/>
              마이페이지에서 관심 카테고리를 설정하시면 맞춤 요약 뉴스를 받아보실 수 있습니다.
            </p>
        """);
        } else {

            for (NewsCategory category : categories) {
                sb.append("<div style=\"margin-bottom: 24px;\">");
                sb.append("<h3 style=\"font-size: 16px; font-weight: bold; margin: 14px 0 8px 0;\">[")
                        .append(category.getLabel())
                        .append("]</h3>");

                List<NewsArticle> articles =
                        newsArticleRepository.findTop5ByCategoryOrderByPublishedAtDesc(category.getLabel());

                if (articles.isEmpty()) {
                    sb.append("""
                    <p style="font-size: 13px; color: #868e96; margin: 4px 0 0 0;">
                      최신 요약 기사가 아직 없습니다.
                    </p>
                """);
                    sb.append("</div>");
                    continue;
                }

                sb.append("<ol style=\"padding-left: 18px; margin: 6px 0 0 0;\">");

                for (NewsArticle article : articles) {
                    sb.append("<li style=\"margin-bottom: 10px;\">");

                    // 제목
                    sb.append("<div style=\"font-size: 14px; font-weight: 600; color: #111111;\">")
                            .append(article.getTitle())
                            .append("</div>");

                    // 요약
                    if (StringUtils.hasText(article.getSummary())) {
                        sb.append("<div style=\"font-size: 13px; color: #495057; margin-top: 3px; line-height: 1.5;\">")
                                .append(article.getSummary().trim())
                                .append("</div>");
                    }

                    // 링크
                    if (StringUtils.hasText(article.getOriginalLink())) {
                        sb.append("<div style=\"margin-top: 4px;\">")
                                .append("<a href=\"")
                                .append(article.getOriginalLink())
                                .append("\" style=\"font-size: 12px; color: #1c7ed6; text-decoration: none;\" target=\"_blank\">")
                                .append("기사 원문 보기")
                                .append("</a></div>");
                    }

                    sb.append("</li>");
                }

                sb.append("</ol>");
                sb.append("</div>");

            }
        }

        // 푸터
        sb.append("""
              <p style="font-size: 12px; color: #868e96; margin-top: 28px; line-height: 1.6;">
                이 메일은 Nuzip 서비스에 가입하신 사용자에게 매일 오전 9시에 발송되는 요약 메일입니다.
              </p>

              <p style="font-size: 13px; color: #111111; margin-top: 20px;">
                - Nuzip 드림
              </p>

            </div>
          </div>
        </div>
        """);

        return sb.toString();
    }

}


