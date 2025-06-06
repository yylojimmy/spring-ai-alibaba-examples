/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.alibaba.cloud.ai.application.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.alibaba.cloud.ai.application.exception.SAAAppException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.memory.ChatMemory;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;


/**
 * @author yuluo
 * @author <a href="mailto:yuluo08290126@gmail.com">yuluo</a>
 */

@Service
public class SAASummarizerService {

	private static final Logger logger = LoggerFactory.getLogger(SAASummarizerService.class);

	private final ChatClient chatClient;

	public SAASummarizerService(
			SimpleLoggerAdvisor simpleLoggerAdvisor,
			MessageChatMemoryAdvisor messageChatMemoryAdvisor,
			@Qualifier("dashscopeChatModel") ChatModel chatModel,
			@Qualifier("summarizerPromptTemplate") PromptTemplate docsSummaryPromptTemplate
	) {

		this.chatClient = ChatClient.builder(chatModel)
				.defaultSystem(
						docsSummaryPromptTemplate.getTemplate()
				).defaultAdvisors(
						messageChatMemoryAdvisor,
						simpleLoggerAdvisor
				).build();
	}

	public Flux<String> summary(MultipartFile file, String url, String chatId) {

		String text = getText(url, file);
		if (!StringUtils.hasText(text)) {
			return Flux.error(new SAAAppException("Invalid file content"));
		}

		return chatClient.prompt()
				.user("Summarize the document")
				.advisors(memoryAdvisor -> memoryAdvisor
						.param(ChatMemory.CONVERSATION_ID, chatId)
				).user(text)
				.stream().content();
	}

	private String getText(String url, MultipartFile file) {

		if (Objects.nonNull(file)) {

			logger.debug("Reading file content form MultipartFile");
			List<Document> documents = new TikaDocumentReader(file.getResource()).get();
			return documents.stream()
					.map(Document::getFormattedContent)
					.collect(Collectors.joining("\n\n"));
		}

		if (StringUtils.hasText(url)) {
			logger.debug("Reading file content form url");
			List<Document> documents = new TikaDocumentReader(url).get();
			return documents.stream()
					.map(Document::getFormattedContent)
					.collect(Collectors.joining("\n\n"));
		}

		return "";
	}

}
