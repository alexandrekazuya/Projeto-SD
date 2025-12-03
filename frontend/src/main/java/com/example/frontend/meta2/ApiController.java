package com.example.frontend.meta2;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import googol.common.dto.SearchResult;

@Controller
public class ApiController {
	@Autowired
	private GatewayServ gatewayServ;

	@GetMapping("/")
	public String mainPage() {
		return "index";
	}

	@GetMapping("/search")
	public String searchPage(@RequestParam(value = "q", defaultValue = "") String query,
			@RequestParam(value = "page", defaultValue = "1") int page,
			Model model) {
		if (query == null)
			query = "";
		try {
			String[] termArr = query.split(",");
			SearchResult[] results = gatewayServ.getGateway().searchWord(termArr, page);
			model.addAttribute("results", results);
			model.addAttribute("query", query);
			model.addAttribute("page", page);
		} catch (Exception e) {
			model.addAttribute("error", e.getMessage());
		}
		return "search";
	}

	@GetMapping("/search/incoming")
	public String searchIncoming(@RequestParam("url") String url, Model model) {
		try {
			String[] incomingLinks = gatewayServ.getGateway().getIncomingLinks(url);
			model.addAttribute("incomingLinks", incomingLinks);
			model.addAttribute("url", url);
		} catch (Exception e) {
			model.addAttribute("error", e.getMessage());
		}
		return "searchinc";
	}

	@PostMapping("/putNew")
	public String putNewPage(@RequestParam("url") String url) {
		try {
			gatewayServ.getGateway().putNewURL(url);
		} catch (Exception e) {
		}
		return "index";
	}

	@GetMapping("/stats/")
	public String statsPage() {
		return "stats";
	}

	@MessageMapping("/stats/refresh")
	@SendTo("/topic/top10")
	public String refreshStats() {
		try {
			return gatewayServ.getGateway().getPlainStatsString();
		} catch (Exception e) {
			return "";
		}
	}
}
