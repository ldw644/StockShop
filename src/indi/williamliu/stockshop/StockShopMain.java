package indi.williamliu.stockshop;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Date;
import java.text.SimpleDateFormat;
import net.milkbowl.vault.economy.EconomyResponse;

import java.io.File;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.sql.ResultSet;

import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.entity.Player;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;

import net.milkbowl.vault.economy.Economy;


public class StockShopMain extends JavaPlugin implements Listener{
	
	private static Economy econ = null;
	BigDecimal hundred = new BigDecimal(100);
	
	@Override
	public void onEnable() {
		getLogger().info("---StockShop---");
		CreateConfig();
		InitDatabase(null);
		InitTable(null);
		Bukkit.getPluginManager().registerEvents(this,this);
		if (!setupEconomy() ) {
			getLogger().info(String.format("[%s] - Disabled due to no Vault dependency found!", getDescription().getName()));
	        getServer().getPluginManager().disablePlugin(this);
	        return;
	        }
	}
	
	private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        econ = rsp.getProvider();
        return econ != null;
    }
	
	private void CreateConfig(){
        if (!new File(getDataFolder() + File.separator + "config.yml").exists()) {
            saveDefaultConfig();
            getLogger().warning("无法找到config.yml,正在创建");
        }
        try {
        	reloadConfig();
            getLogger().info("成功加载config");
        }
        catch (Exception e) {
            e.printStackTrace();
            getServer().getPluginManager().disablePlugin(this);
            getLogger().severe("无法读取config");
        }
	}
	

	public static boolean isNumeric(String str) {
		for(int i = 0; i < str.length(); i++) {
			System.out.println(str.charAt(i));
			if (!Character.isDigit(str.charAt(i))){
				return false;
			}
		}
		return true;
	}

	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args){
		if (cmd.getName().equalsIgnoreCase("ss")){
			if(sender instanceof Player) {
				Player player = (Player) sender;
				for(int i = 0;i<args.length;i++) {
					if(args[i].equalsIgnoreCase("hand")){
						ItemStack stack = player.getItemInHand();
						String name = stack.getType().toString();
						//System.out.println(name);
						args[i] = name;
					}
				}
			}
			if(args.length == 0 || (args.length == 1 && args[0].equalsIgnoreCase("help") || (args.length == 2 && args[0].equalsIgnoreCase("help") && args[1].equals("1")))) {
				sender.sendMessage(ChatColor.GOLD + "StockShop 管理员帮助界面 - 当前页码 1/2");
				sender.sendMessage(ChatColor.GOLD + "所有的" + ChatColor.GREEN + "<物品名>" + ChatColor.GOLD + "均可以用" + ChatColor.GREEN + "hand" + ChatColor.GOLD + "来指代手中的物品");
				sender.sendMessage(ChatColor.GOLD + "==========" + ChatColor.GREEN + "<>" + ChatColor.GOLD
						+ "代表必填，" + ChatColor.GREEN + "[]" + ChatColor.GOLD + "代表选填==========");
				sender.sendMessage(ChatColor.GREEN + "/ss help " + ChatColor.GOLD + "- 打开本帮助页面");
				sender.sendMessage(ChatColor.GREEN + "/ss all " + ChatColor.GOLD + "- 查看现有的全部市场");
				sender.sendMessage(ChatColor.GREEN + "/ss create <物品名> " + ChatColor.GOLD + "- 创建一个市场");
				sender.sendMessage(ChatColor.GREEN + "/ss enable/disable <物品名> " + ChatColor.GOLD + "- 启用/禁用一个市场");
				sender.sendMessage(ChatColor.GREEN + "/ss status <物品名> " + ChatColor.GOLD + "- 查看某物品的十档委托价");
				sender.sendMessage(ChatColor.GREEN + "/ss buy/sell <物品名> <数量> <单个价格> " + ChatColor.GOLD + "- 委托购买/出售 ");
				sender.sendMessage(ChatColor.GOLD + "          第1页 共2页 前往第二页：/ss help 2");
				
				return true;
			}
			else if(args.length == 2 && args[0].equalsIgnoreCase("help") && args[1].equals("2")){
				sender.sendMessage(ChatColor.GOLD + "StockShop 管理员帮助界面 - 当前页码 2/2");
				sender.sendMessage(ChatColor.GOLD + "所有的" + ChatColor.GREEN + "<物品名>" + ChatColor.GOLD + "均可以用" + ChatColor.GREEN + "hand" + ChatColor.GOLD + "来指代手中的物品");
				sender.sendMessage(ChatColor.GOLD + "==========" + ChatColor.GREEN + "<>" + ChatColor.GOLD
						+ "代表必填，" + ChatColor.GREEN + "[]" + ChatColor.GOLD + "代表选填==========");
				sender.sendMessage(ChatColor.GREEN + "/ss queryall [玩家名] " + ChatColor.GOLD + "- 查询对应玩家的所有委托 " + ChatColor.LIGHT_PURPLE + "[玩家名] 空缺为查询自己");
				sender.sendMessage(ChatColor.GREEN + "/ss query/queryreceive [玩家名] " + ChatColor.GOLD + "- 查询对应玩家的未成交委托/未领取物品委托");
				sender.sendMessage(ChatColor.GREEN + "/ss recall <委托合同号> " + ChatColor.GOLD + "- 撤回<委托合同号>对应的未成交的委托");
				sender.sendMessage(ChatColor.GREEN + "/ss receive <委托合同号> " + ChatColor.GOLD + "- 领取<委托合同号>对应的委托已成交/已返还的物品");
				sender.sendMessage(ChatColor.GREEN + "/ss gui " + ChatColor.GOLD + "- 打开图形化界面");
				sender.sendMessage(" ");
				sender.sendMessage(ChatColor.GOLD + "          第2页 共2页 前往第一页：/ss help 1");
				return true;
			}
			else if(args[0].equalsIgnoreCase("create")) {
				if(args.length == 2) {
					args[1] = args[1].toUpperCase();
					Material item = Material.getMaterial(args[1]);
					if(item == null) {
						sender.sendMessage(ChatColor.RED + args[1] + " is not a valid item.");
						return true;
					}
					else {
						if(sender.hasPermission("stockshop.admin")){							
							String key = "Market." + args[1] + ".Enable";
							if(!getConfig().contains(key)) {
								getConfig().set(key, true);
								saveConfig();
								sender.sendMessage(ChatColor.GREEN + args[1] + " Market Created Successfully!");
							}
							else {
								sender.sendMessage(ChatColor.YELLOW + args[1] + " Market has already exist!");
							}
						}
						else {
							sender.sendMessage(ChatColor.RED + "You don't have the permission to do that!") ;
						}
						return true;
					}
				}
				else {
					return false;
				}
			}
			else if(args[0].equalsIgnoreCase("enable")) {
				if(args.length == 2) {
					args[1] = args[1].toUpperCase();
					if(sender.hasPermission("stockshop.admin")) {
						String key = "Market." + args[1] + ".Enable";
						if(getConfig().contains(key) && getConfig().getBoolean(key) == false) {
							getConfig().set(key, true);
							saveConfig();
							sender.sendMessage(ChatColor.GREEN + args[1] + " Market Enabled Successfully!");
						}
						else if(getConfig().contains(key) && getConfig().getBoolean(key) == true) {
							sender.sendMessage(ChatColor.GREEN + args[1] + " Market has already enabled!");
						}
						else {
							sender.sendMessage(ChatColor.YELLOW + args[1] + " Market doesn't exist!");
						}
						return true;
					}
					else if(!sender.hasPermission("stockshop.admin")) {
						sender.sendMessage(ChatColor.RED + "You don't have permission to do that!");
					}
				}
				else {
					return false;
				}
			}
			else if(args[0].equalsIgnoreCase("disable")) {
				if(args.length == 2) {
					args[1] = args[1].toUpperCase();
					if(sender.hasPermission("stockshop.admin")){							
						String key = "Market." + args[1] + ".Enable";
						if(getConfig().contains(key) && getConfig().getBoolean(key) == true) {
							getConfig().set(key, false);
							saveConfig();
							sender.sendMessage(ChatColor.GREEN + args[1] + " Market Disabled Successfully!");
						}
						else if(getConfig().contains(key) && getConfig().getBoolean(key) == false) {
							sender.sendMessage(ChatColor.GREEN + args[1] + " Market has already disabled!");
						}
						else {
							sender.sendMessage(ChatColor.YELLOW + args[1] + " Market doesn't exist!");
						}
						return true;
					}
					else {
						sender.sendMessage(ChatColor.RED + "You don't have the permission to Create Market");
						return true;
					}
				}
				else {
					return false;
				}
			}
			else if(args[0].equalsIgnoreCase("all")) {
				if(args.length == 1) {
					Set set = this.getConfig().getConfigurationSection("Market").getKeys(false);
					Iterator<String> it = set.iterator();
					String str = "";
					while(it.hasNext()){
						str += it.next();
						str += ' ';
					}
					sender.sendMessage(str);
				}
				else {
					return false;
				}
				return true;
			}
			else if(args[0].equalsIgnoreCase("status")) {
				if(args.length == 2) {
					String item = args[1].toUpperCase();
					String key = "Market." + item + ".Enable";
					if(getConfig().contains(key)) {
						Connection c = null;
					    Statement stmt = null;
					    try {
					        Class.forName("org.sqlite.JDBC");
					        c = DriverManager.getConnection(String.format("jdbc:sqlite:%s",  getDataFolder() + File.separator + "StockShop.db"));
					        stmt = c.createStatement();
					        ResultSet rs = null;
					        
					        rs = stmt.executeQuery("SELECT GOODS, TYPE, PRICE, NOT_DEAL_AMOUNT, RECALL FROM ENTRUSTMENT "
					        		+ String.format("WHERE GOODS = '%s' AND TYPE = 'SELL' AND NOT_DEAL_AMOUNT <> 0 AND RECALL = 0 ORDER BY PRICE ASC;",item));
					        int cnt = -1;
					        double prevPrice = 0;
					        double[] priceList_s = new double[] {0,0,0,0,0};//index0 卖一...index4卖5
					        int[] amountList_s = new int[] {0,0,0,0,0};
					        while(rs.next()) {
					        	BigDecimal q_price_100 = new BigDecimal(rs.getInt("PRICE"));
					        	double q_price = q_price_100.divide(hundred).doubleValue();
					        	int q_not_deal_amount = rs.getInt("NOT_DEAL_AMOUNT");
					        	if(q_price != prevPrice) {
					        		cnt++;
					        		if(cnt == 5) break;
					        		priceList_s[cnt] = q_price;
					        		amountList_s[cnt] = q_not_deal_amount;
					        	}
					        	else if(q_price == prevPrice) {
					        		amountList_s[cnt] += q_not_deal_amount;
					        	}
					        	prevPrice = q_price;	
					        }
					        rs.close();
					        
					        rs = stmt.executeQuery("SELECT GOODS, TYPE, PRICE, NOT_DEAL_AMOUNT, RECALL FROM ENTRUSTMENT "
					        		+ String.format("WHERE GOODS = '%s' AND TYPE = 'BUY' AND NOT_DEAL_AMOUNT <> 0 AND RECALL = 0 ORDER BY PRICE DESC;",item));
					        cnt = -1;
					        prevPrice = 0;
					        double[] priceList_b = new double[] {0,0,0,0,0};//index0买一 index4买五
					        int[] amountList_b = new int[] {0,0,0,0,0};
					        while(rs.next()) {
					        	BigDecimal q_price_100 = new BigDecimal(rs.getInt("PRICE"));
					        	double q_price = q_price_100.divide(hundred).doubleValue();
					        	int q_not_deal_amount = rs.getInt("NOT_DEAL_AMOUNT");
					        	if(q_price != prevPrice) {
					        		cnt++;
					        		if(cnt == 5) break;
					        		priceList_b[cnt] = q_price;
					        		amountList_b[cnt] = q_not_deal_amount;
					        	}
					        	else if(q_price == prevPrice) {
					        		amountList_b[cnt] += q_not_deal_amount;
					        	}
					        	prevPrice = q_price;	
					        }
					        rs.close();
					        stmt.close();
					        c.close();
					        
					        sender.sendMessage(
					        		String.format(ChatColor.GREEN + "卖五" + ChatColor.WHITE + "    %.2f    %d\n",priceList_s[4],amountList_s[4])
					        		+ String.format(ChatColor.GREEN + "卖四" + ChatColor.WHITE + "    %.2f    %d\n",priceList_s[3],amountList_s[3])
					        		+ String.format(ChatColor.GREEN + "卖三" + ChatColor.WHITE + "    %.2f    %d\n",priceList_s[2],amountList_s[2])
					        		+ String.format(ChatColor.GREEN + "卖二" + ChatColor.WHITE + "    %.2f    %d\n",priceList_s[1],amountList_s[1])
					        		+ String.format(ChatColor.GREEN + "卖一" + ChatColor.WHITE + "    %.2f    %d___物品：%s\n",priceList_s[0],amountList_s[0],item)
					        		+ String.format(ChatColor.RED + "买一" + ChatColor.WHITE + "    %.2f    %d\n",priceList_b[0],amountList_b[0])
					        		+ String.format(ChatColor.RED + "买二" + ChatColor.WHITE + "    %.2f    %d\n",priceList_b[1],amountList_b[1])
					        		+ String.format(ChatColor.RED + "买三" + ChatColor.WHITE + "    %.2f    %d\n",priceList_b[2],amountList_b[2])
					        		+ String.format(ChatColor.RED + "买四" + ChatColor.WHITE + "    %.2f    %d\n",priceList_b[3],amountList_b[3])
					        		+ String.format(ChatColor.RED + "买五" + ChatColor.WHITE + "    %.2f    %d\n",priceList_b[4],amountList_b[4])
					        		);
					        return true;
					    }
					    catch ( Exception e ) {
					        System.err.println( e.getClass().getName() + ": " + e.getMessage() );
					        return false;
					    }
					    
					}
					else if(!getConfig().contains(key)) {
						sender.sendMessage(ChatColor.RED + "未找到该市场！");
						return true;
					}
				}
				else if(args.length == 1) {
					sender.sendMessage("/ss status <Item> - 查看某市场的十档委托价");
					return true;
				}
				else if(args.length > 2) {
					return false;
				}
			}
			//buy 购买 用法/ss buy <Gold> <amount> <singlePrice>
			else if(args[0].equalsIgnoreCase("buy") || args[0].equalsIgnoreCase("sell")) {//如果输入代码为buy或sell
				if(sender instanceof Player) {//如果发出者是玩家
					Player player = (Player) sender;
					if(args.length == 4) {
						args[1] = args[1].toUpperCase();
						int digitConfig = getConfig().getInt("Digit");
						String[] digit = args[3].split("\\.");
						if(digitConfig == -1) {
							int pass;
						}
						else if(digit.length == 2 && digit[1].length() > digitConfig) {
							sender.sendMessage(String.format(ChatColor.DARK_RED + "委托价格不能超过%d位小数！",digitConfig));
							return true;
						}
						String key = "Market." + args[1] + ".Enable";
						if(getConfig().contains(key) && getConfig().getBoolean(key) == true) {//check if 在市场中
							int amount = Integer.parseInt(args[2]);
							BigDecimal singlePrice = new BigDecimal(String.valueOf(args[3]));
							
							int singlePrice_100 = singlePrice.multiply(hundred).intValue();//100times price
							if(amount <= 0) {
								sender.sendMessage(ChatColor.RED + "Amount must be a positive number.");
							}
							else if(singlePrice_100 <= 0 ) {
								sender.sendMessage(ChatColor.RED + "Price must be a positive number.");
							}
							if(sender.hasPermission("stockshop.trade")) {
								BigDecimal totalPrice_100 = new BigDecimal(amount * singlePrice_100);
								double totalPrice = totalPrice_100.divide(hundred).doubleValue();
								if(args[0].equalsIgnoreCase("buy")) {
									if(econ.has(player, totalPrice)){
										EconomyResponse r = econ.withdrawPlayer(player, totalPrice);
										if(r.transactionSuccess()) {
											SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
									    	String time = df.format(new Date());
											int EntrustmentID = InsertEntrustment(args[1], "BUY", amount, singlePrice_100, 0, player.getName(),time);
											int[] array = TradeBuy(args[1],amount,singlePrice_100,EntrustmentID);
											BigDecimal diff_100 = new BigDecimal(array[1]-array[0]);
											double diff = diff_100.divide(hundred).doubleValue();
											econ.depositPlayer(player, diff);
											sender.sendMessage(ChatColor.YELLOW + "Successfully offer");
											return true;
										}
										else {
							                sender.sendMessage(String.format("发生了一个错误: %s", r.errorMessage));
							                return true;
										}
									}
									else {
										sender.sendMessage(ChatColor.RED + "You don't have enough money!");
										return true;
									}
								}
								
								else if(args[0].equalsIgnoreCase("sell")) {//未完成 检测是否有足够的物品
									ItemStack stack = new ItemStack(Material.getMaterial(args[1]),amount);
									Material mat = Material.getMaterial(args[1]);
									Inventory inv = player.getInventory();
									if(inv.contains(mat,amount)) {
										inv.removeItem(stack);
									}
									else if(!inv.contains(mat,amount)) {
										sender.sendMessage(ChatColor.RED + "You don't have enough items!");
										return true;
									}
									SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
							    	String time = df.format(new Date());
									int EntrustmentID = InsertEntrustment(args[1], "SELL", amount, singlePrice_100, 0, player.getName(), time);
									TradeSell(args[1],amount,singlePrice_100,EntrustmentID);
									sender.sendMessage(ChatColor.YELLOW + "Successfully offer");
									
									return true;
								}
							}
							else {
								sender.sendMessage(ChatColor.RED + "You don't have the permission to Trade");
							}
							return true;
						}
						else if(getConfig().contains(key) == false) {
							sender.sendMessage(ChatColor.RED + args[1] + " Market doesn't exist!");
							return true;
						}
						else if(getConfig().contains(key) && getConfig().getBoolean(key) == false) {
							sender.sendMessage(ChatColor.RED + args[1] + " Market has disabled!");
							return true;
						}
					}
					else{
						sender.sendMessage(ChatColor.RED + "usage:/ss [buy/sell] [Item] [Amount] [SinglePrice]");
						return true;
					}
				}
				else {
					sender.sendMessage(ChatColor.RED + "Console cannot run this command!");
					return true;
				}
			}
			else if(args[0].equalsIgnoreCase("queryreceive") || args[0].equalsIgnoreCase("queryall") || args[0].equalsIgnoreCase("query")) {//查询所有
				String name;
				if(args.length == 1 && sender instanceof Player) {
					Player player = (Player) sender;
					name = player.getName();
				}
				else if(args.length == 1 && !(sender instanceof Player)) {
					sender.sendMessage(ChatColor.RED + "Console cannot run this command!\nPlease use </ss [queryall/query] [name]> to query player's info.");
					return true;
				}
				else if(args.length == 2 && !sender.hasPermission("stockshop.admin")) {
					sender.sendMessage(ChatColor.RED + "You don't have permission to do that!");
					return true;
				}
				else {
			    	name = args[1];
			    }
				sender.sendMessage("----------"+name+"----------");
				Connection c = null;
			    Statement stmt = null;
			    try {
			        Class.forName("org.sqlite.JDBC");
			        c = DriverManager.getConnection(String.format("jdbc:sqlite:%s",  getDataFolder() + File.separator + "StockShop.db"));
			        stmt = c.createStatement();
			        ResultSet rs = null;
			        if (args[0].equalsIgnoreCase("queryall")){
			        	rs = stmt.executeQuery("SELECT * FROM ENTRUSTMENT "+
			        	String.format("WHERE PLAYER = '%s' ",name)+
			        	"ORDER BY ID ASC");
			        }
			        else if (args[0].equalsIgnoreCase("query")) {
			        	rs = stmt.executeQuery("SELECT * FROM ENTRUSTMENT "+
					        	String.format("WHERE PLAYER = '%s' AND NOT_DEAL_AMOUNT <> 0 AND RECALL = 0 ",name)+
					        	"ORDER BY ID ASC");
			        }
			        else if (args[0].equalsIgnoreCase("queryreceive")) {
			        	rs = stmt.executeQuery("SELECT * FROM ENTRUSTMENT "+
					        	String.format("WHERE PLAYER = '%s' AND NOT_RECEIVE_AMOUNT <> 0 ",name)+
					        	"ORDER BY ID ASC");
			        }
			        int rs_num = 0;
			        int cnt = 0;
			        while(rs.next()) {
			        	cnt++;
			        	int q_id = rs.getInt("ID");
			        	String q_goods = rs.getString("GOODS");
			        	String q_type = rs.getString("TYPE");
			        	int q_amount = rs.getInt("AMOUNT");
			        	BigDecimal q_price_100 = new BigDecimal(rs.getInt("PRICE"));
			        	double q_price = q_price_100.divide(hundred).doubleValue();
			        	int q_deal_amount = rs.getInt("DEAL_AMOUNT");
			        	int q_not_deal_amount = rs.getInt("NOT_DEAL_AMOUNT");
			        	int q_not_receive_amount = rs.getInt("NOT_RECEIVE_AMOUNT");
			        	int q_recall = rs.getInt("RECALL");
			        	String q_player = rs.getString("PLAYER");
			        	String q_time = rs.getString("TIME");
			        	rs_num += 1;
			        	String msg = String.format(ChatColor.BLUE + "第%d条：\n"+ ChatColor.WHITE + "委托编号：%d 交易类型：%s 交易商品：%s 交易数量：%d 交易价格：%.2f\n已成交数量：%d 未成交数量：%d 未领取数量：%d 是否已撤单：%d 委托时间：%s",rs_num,q_id,q_type,q_goods,q_amount,q_price,q_deal_amount,q_not_deal_amount,q_not_receive_amount,q_recall,q_time);
			        	sender.sendMessage(msg);
			        }
			        if(cnt == 0 && args[0].equalsIgnoreCase("query")) {
			        	sender.sendMessage("未找到未完成交易！");
			        }
			        else if(cnt == 0 && args[0].equalsIgnoreCase("queryall")) {
			        	sender.sendMessage("未找到历史交易！");
			        }
			        else if(cnt == 0 && args[0].equalsIgnoreCase("queryreceive")) {
			        	sender.sendMessage("未找到未领取物品的交易！");
			        }
			        sender.sendMessage("----------"+name+"----------");
			        rs.close();
			        stmt.close();
			        c.close();
			    	}
			    catch ( Exception e ) {
			        System.err.println( e.getClass().getName() + ": " + e.getMessage() );
			    }
			    return true;
			}
			else if(args[0].equalsIgnoreCase("recall")) {
				if(args.length != 2) {
					return false;
				}
				
				Connection c = null;
			    Statement stmt = null;
			    try {
			        Class.forName("org.sqlite.JDBC");
			        c = DriverManager.getConnection(String.format("jdbc:sqlite:%s",  getDataFolder() + File.separator + "StockShop.db"));
			        stmt = c.createStatement();

			        ResultSet rs = stmt.executeQuery(String.format("SELECT ID, TYPE, NOT_DEAL_AMOUNT, PLAYER, PRICE FROM ENTRUSTMENT WHERE ID ='%s' LIMIT 1",args[1]));
			        if(rs.next()) {
			        	if(!sender.hasPermission("stockshop.admin")) {
			        		Player player = (Player) sender;
			        		if(!player.getName().equals(rs.getString("PLAYER"))) {
			        			sender.sendMessage(ChatColor.RED + "You don't have permission to do that!");
			        			return true;
			        		}
			        	}
			        	if(rs.getInt("NOT_DEAL_AMOUNT") == 0) {
			        		sender.sendMessage(ChatColor.RED + "Failed! Your Entrustment has been Completed!");
			        		return true;
			        	}
			        	if (rs.getString("TYPE").equals("BUY")){
			        		BigDecimal recallPrice_100 = new BigDecimal(rs.getInt("NOT_DEAL_AMOUNT") * rs.getInt("PRICE"));
				        	double recallPrice = recallPrice_100.divide(hundred).doubleValue();
				        	econ.depositPlayer(rs.getString("PLAYER"),recallPrice);
			        	}
			        	else if (rs.getString("TYPE").equals("SELL")) {
			        		int notDealAmount = rs.getInt("NOT_DEAL_AMOUNT");
			        		String sql = String.format("UPDATE ENTRUSTMENT SET NOT_RECEIVE_AMOUNT = NOT_RECEIVE_AMOUNT + %d WHERE ID = '%s'", notDealAmount,args[1]);
			        		stmt.executeUpdate(sql);
			        		//把东西还给人家
			        	}
			        }
			        else {
			        	sender.sendMessage(ChatColor.RED + "Failed! Please check your EntrustmentID!");
			        	return true;
			        }			        
				    rs.close();
		        	String sql = String.format("UPDATE ENTRUSTMENT SET RECALL = '1' WHERE ID = '%s'",args[1]);
		        	stmt.executeUpdate(sql);
				    stmt.close();
				    c.close();
				    return true;
			    }
			    catch ( Exception e ) {
			        System.err.println( e.getClass().getName() + ": " + e.getMessage() );
			        return false;
			    }
			}
			else if(args[0].equalsIgnoreCase("receive")) {
				if(args.length == 2) {
					if(!(sender instanceof Player)) {
						sender.sendMessage(ChatColor.RED + "Console can't run this command!");
						return true;
					}
					else {
						sender.sendMessage("0");
						Player player = (Player) sender;
						PlayerInventory inventory = player.getInventory();
						Connection c = null;
					    Statement stmt = null;
					    ResultSet rs = null;
						try {
					        Class.forName("org.sqlite.JDBC");
					        c = DriverManager.getConnection(String.format("jdbc:sqlite:%s",  getDataFolder() + File.separator + "StockShop.db"));
					        stmt = c.createStatement();
					        
					        String goods = null;
					        int notReceiveAmount = 0;
					        int id = 0;
					        String name = null;
					        HashMap<Integer, ItemStack> overfill;
					        ItemStack overfillStack;
					        int overfillAmount = 0;
					        String sql;
					        
					        rs = stmt.executeQuery(String.format("SELECT ID, GOODS, NOT_RECEIVE_AMOUNT, PLAYER FROM ENTRUSTMENT WHERE ID = '%s' LIMIT 1",args[1]));
					        if(rs.next()) {
					        	id = rs.getInt("ID");
					        	goods = rs.getString("GOODS");
					        	notReceiveAmount = rs.getInt("NOT_RECEIVE_AMOUNT");
					        	name = rs.getString("PLAYER");
					        	if(player.getName().equals(name)) {
					        		if(notReceiveAmount == 0) {
						        		sender.sendMessage(ChatColor.RED + "Nothing could receive!");
						        		rs.close();
										stmt.close();
										c.close();
										return true;
						        	}
					        		ItemStack stack = new ItemStack(Material.getMaterial(goods),notReceiveAmount);
					        		overfill = inventory.addItem(stack);
					        		if(overfill.toString() == "{}") {
					        			sender.sendMessage(overfill.toString());
					        			overfillStack = overfill.get(0);
					        			sender.sendMessage(overfillStack.toString());
					        			overfillAmount = overfillStack.getAmount();
					        			sender.sendMessage(String.format(ChatColor.YELLOW + "Not enough cell! %d %s remained.", overfillAmount, goods));
					        		}
					        		else if (overfill.toString() == "{}") {
					        			sender.sendMessage(ChatColor.GREEN + "Success!");
					        		}
					        		sql = String.format("UPDATE ENTRUSTMENT SET NOT_RECEIVE_AMOUNT = %d WHERE ID = %d",overfillAmount,id);
					        		stmt.executeUpdate(sql);
					        	}
					        	else {
					        		sender.sendMessage(ChatColor.RED + "You cannot receive other player's items!");
					        		return true;
					        	}
					        }
					        else {
					        	sender.sendMessage(ChatColor.RED + "Failed! Please check your EntrustmentID!");
					        	return true;
					        }
					        rs.close();
							stmt.close();
							c.close();
							return true;
						}
						catch ( Exception e ) {
					        System.err.println( e.getClass().getName() + ": " + e.getMessage() );
					        return false;
					    }
					}
					
					
					
				}
				else if(args.length != 2) {
					return false;
				}
			}
			else if(args[0].equalsIgnoreCase("gui")) {
				if (sender instanceof Player) {
					Player p = (Player) sender; 
					if(args.length == 1) {
						Player player = (Player) sender;
						InventoryHolder holder = (InventoryHolder) sender;
						Inventory inv = Bukkit.createInventory(holder, 6*9, "StockShop 主菜单");
						
						ItemStack is0 = new ItemStack(Material.APPLE);
						ItemMeta im0 = is0.getItemMeta();
						im0.setDisplayName("查看所有市场");
						is0.setItemMeta(im0);
						inv.setItem(0, is0);
						/*
						ItemStack is1 = new ItemStack(Material.BREAD);
						ItemMeta im1 = is1.getItemMeta();
						im1.setDisplayName("查看所有市场2");
						is1.setItemMeta(im1);
						inv.setItem(1, is0);
						*/
						player.openInventory(inv);
						return true;
					}
					else if(args.length >= 2) {
						if(args[1].equalsIgnoreCase("all")) {//ss gui all
							String key;
							key = "Market";
							Set<String> allMarket = getConfig().getConfigurationSection(key).getKeys(false);
							Iterator allMarketIter = allMarket.iterator();
							int cnt = 0;
							int page = 1;
							InventoryHolder holder = (InventoryHolder) sender;
							
							List<Inventory> allInv = new ArrayList<>();
							Inventory inv = null;
							ItemStack Pink_dye = new ItemStack(Material.INK_SACK,1,(short)9);
							ItemStack Lime_dye = new ItemStack(Material.INK_SACK,1,(short)10);
							ItemStack AIR = new ItemStack(Material.AIR);
							while(allMarketIter.hasNext()) {
								if (cnt == 0) {
									inv = Bukkit.createInventory(holder, 6*9, "StockShop 全部市场 - 第" + page + "页");
									if (page == 1) {
										inv.setItem(48, AIR);
									}
									else if (page != 1) {
										inv.setItem(48, Pink_dye);
									}
								}
								String name = allMarketIter.next().toString();
								ItemStack stack = new ItemStack(Material.getMaterial(name),1);
								ItemMeta meta = stack.getItemMeta();
								meta.setDisplayName(name + " 市场");
								List<String> lore = new ArrayList<>();
								lore.add(ChatColor.GOLD + "点击进入市场");
								meta.setLore(lore);
								inv.setItem(cnt, stack);
								if(cnt == 44) {
									if(allMarketIter.hasNext()) {
										page++;
										cnt = 0;
										inv.setItem(50, Lime_dye);
										allInv.add(inv);
									}
									else if(!allMarketIter.hasNext()) {
										inv.setItem(50, AIR);
										allInv.add(inv);
									}
								}
								else if(cnt<44 && allMarketIter.hasNext()) {
									cnt++;
								}
								else if(cnt<44 && !allMarketIter.hasNext()) {
									//inv.setItem(50, RSOff);
									inv.setItem(50, new ItemStack(AIR));
									allInv.add(inv);
								}
							}
							p.closeInventory();
							
							if(args.length == 3){//ss gui all 2(pages)
								if(isNumeric(args[2])) {
									try {
										p.openInventory(allInv.get(Integer.parseInt(args[2]) - 1));
										return true;
									}
									catch(Exception e) {
										sender.sendMessage(ChatColor.RED + "此页码不存在！");
										return true;
									}
								}
								else if(!isNumeric(args[2])) {
									sender.sendMessage(ChatColor.RED + "输入的页码必须为数字！");
									return true;
								}
							}
							else if(args.length == 2) {
								p.openInventory(allInv.get(0));
								return true;
							}
							
						}
						else if(args[1].equalsIgnoreCase("status")) {//ss gui status [itemName]
							if(args.length == 3) {
								Set set = getConfig().getConfigurationSection("Market").getKeys(false);
								Iterator iter = set.iterator();
								Boolean marketExist = false;
								while(iter.hasNext()) {
									if(iter.next().equals(args[2])) {
										marketExist = true;
										break;
									}
								}
								if (marketExist == true) {
									/*
									List<double[]> list = Status(args[2]);
									double[] priceList_s = list.get(0);
									double[] amountList_s = list.get(1);
									double[] priceList_b = list.get(2);
									double[] amountList_b = list.get(3);
									*/
									p.performCommand(String.format("ss status %s",args[2]));
									return true;
								}
								else if (marketExist == false) {
									sender.sendMessage(ChatColor.RED + "输入的市场不存在！");
								}
							}
							else if(args.length != 3) {
								sender.sendMessage(ChatColor.RED + "输入格式错误！");
								return true;
							}
						}
					}
				}
				else if (!(sender instanceof Player)) {
					sender.sendMessage(ChatColor.RED + "Console can't run this command!");
					return true;
				}	
			}
			return false;
		}
		return false;
	}
	

	public boolean InitDatabase(String args[]) {
		Connection c = null;
		try {
			 Class.forName("org.sqlite.JDBC");
			 c = DriverManager.getConnection(String.format("jdbc:sqlite:%s",  getDataFolder() + File.separator + "StockShop.db"));
			 return true;
		} 
		catch ( Exception e ) {
		     System.err.println( e.getClass().getName() + ": " + e.getMessage() );
		     return false;
		}
	}
	
	public boolean InitTable(String args[]) {
		Connection c = null;
	    Statement stmt = null;
	    try {
	        Class.forName("org.sqlite.JDBC");
	        c = DriverManager.getConnection(String.format("jdbc:sqlite:%s",  getDataFolder() + File.separator + "StockShop.db"));
	        stmt = c.createStatement();
	        
	        
	        ResultSet rs = stmt.executeQuery("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='ENTRUSTMENT';");
	        int cnt = 0;
	        while(rs.next()) {
	        	cnt = rs.getInt("count(*)");
	        }
	        if (cnt == 0) {
		        String sql;
		        sql = 
				 "CREATE TABLE ENTRUSTMENT (" +
				 "ID	INTEGER	PRIMARY KEY	AUTOINCREMENT," +
				 "GOODS	TEXT," + 
				 "TYPE	TEXT, " + //type: BUY SELL
				 "AMOUNT	INT," +
				 "DEAL_AMOUNT	INT,"+
				 "NOT_DEAL_AMOUNT	INT,"+
				 "NOT_RECEIVE_AMOUNT	INT,"+
				 "PRICE	INT," +
				 "RECALL	INT,"+//是否撤回 1/0
				 "PLAYER	TEXT,"+
				 "TIME	TEXT);";
		                     
		        stmt.executeUpdate(sql);  
	        }
	        stmt.close();
	        c.close();
	        return true;
	    }
	    catch ( Exception e ) {
	        System.err.println( e.getClass().getName() + ": " + e.getMessage() );
	        return false;
	    }
	}
	
	public int InsertEntrustment(String goods,String type,int amount,int price,int recall,String player, String time) {
		Connection c = null;
	    Statement stmt = null;
	    try {
	      Class.forName("org.sqlite.JDBC");
	      c = DriverManager.getConnection(String.format("jdbc:sqlite:%s",  getDataFolder() + File.separator + "StockShop.db"));
	      c.setAutoCommit(false);
	      stmt = c.createStatement();
	      String sql;

	      sql = "INSERT INTO ENTRUSTMENT " +
	    		  String.format("VALUES (null, '%s', '%s', %d, 0, %d, 0, %d, %d, '%s','%s');",goods,type,amount,amount,price,recall,player,time); 
	      stmt.executeUpdate(sql);
	      
	      ResultSet rs = stmt.executeQuery("SELECT MAX(ID) FROM ENTRUSTMENT");
	      int cnt=0;
	      while(rs.next()) {
	    	  cnt = rs.getInt("MAX(ID)");
	      }//返回委托编号
	      
	      rs.close();
	      stmt.close();
	      c.commit();
	      c.close();
	      return cnt;
	    } catch ( Exception e ) {
	      System.err.println( e.getClass().getName() + ": " + e.getMessage() );
	      return 0;
	    }
	}
	
/*	
	public boolean InitBSTable(String goods) {
		Connection c = null;
	    Statement stmt = null;
	    try {
	        Class.forName("org.sqlite.JDBC");
	        c = DriverManager.getConnection(String.format("jdbc:sqlite:%s",  getDataFolder() + File.separator + "StockShop.db"));
	        stmt = c.createStatement();
	        
	        
	        ResultSet rs = stmt.executeQuery(String.format("SELECT count(*) FROM sqlite_master WHERE type='table' AND name='%s_B';",goods));
	        int cnt = 0;
	        while(rs.next()) {
	        	cnt = rs.getInt("count(*)");
	        }
	        if (cnt == 0) {
		        String sql;
		        sql = String.format("CREATE TABLE %s_B (",goods) +
	                         "ID	INTEGER	PRIMARY KEY	AUTOINCREMENT," +
		                     "PRICE	INT," + 
		                     "AMOUNT	INT, " +
		                     "DEAL_AMOUNT	INT," +
		                     "NOT_DEAL_AMOUNT	INT," +
		                     "DEAL	INT," +//是否完全成交 1/0
		                     "RECALL	INT,"+//是否撤回 1/0
		                     "PLAYER	TEXT,"+
		                     "TIME	TEXT,"+
		                     "ENTRUSTMENT_ID	INTEGER,"+
		                     "FOREIGN KEY (ENTRUSTMENT_ID) REFERENCES ENTRUSTMENT(ID));";
		                     
		        stmt.executeUpdate(sql);  
		        
		        sql = String.format("CREATE TABLE %s_S (",goods) +
                        "ID	INTEGER	PRIMARY KEY	AUTOINCREMENT," +
	                     "PRICE	INT," + 
	                     "AMOUNT	INT, " +
	                     "DEAL_AMOUNT	INT," +
	                     "NOT_DEAL_AMOUNT	INT," +
	                     "DEAL	INT," +//是否完全成交 1/0
	                     "RECALL	INT,"+//是否撤回 1/0
	                     "PLAYER	TEXT,"+
	                     "TIME	TEXT,"+
		        		"ENTRUSTMENT_ID	INTEGER,"+
		        		"FOREIGN KEY (ENTRUSTMENT_ID) REFERENCES ENTRUSTMENT(ID));";
		        
		        stmt.executeUpdate(sql);
	        }
	        stmt.close();
	        c.close();
	        return true;
	    }
	    catch ( Exception e ) {
	        System.err.println( e.getClass().getName() + ": " + e.getMessage() );
	        System.exit(0);
	        return false;
	    }
	}
*/
	
	public int[] TradeBuy(String goods,int amount,int singlePrice_100,int EntrustmentID) {
		Connection c = null;
	    Statement stmt = null;
	    int totalDealPrice = 0;//总成交价
	    int totalEntrustmentPrice = 0;//总委托价
	    try {
	        Class.forName("org.sqlite.JDBC");
	        c = DriverManager.getConnection(String.format("jdbc:sqlite:%s",  getDataFolder() + File.separator + "StockShop.db"));
	        stmt = c.createStatement();
	        
	        
	        ResultSet rs = stmt.executeQuery("SELECT ID, PRICE, DEAL_AMOUNT, NOT_DEAL_AMOUNT, RECALL "+
	        								"FROM ENTRUSTMENT "+
	        								String.format("WHERE GOODS = '%s' AND TYPE = 'SELL' AND NOT_DEAL_AMOUNT <> 0 AND RECALL = '0' ",goods)+
	        								"ORDER BY PRICE ASC, ID ASC;");
	        int not_deal_amount = amount;//主买的未交易量
	        
	        List<List> lst = new ArrayList<>();
	        String playerName = "";
	        while(rs.next()) {
	        	List<Integer> row = new ArrayList<>();
	        	row.add(rs.getInt("PRICE"));
	        	row.add(rs.getInt("DEAL_AMOUNT"));
	        	row.add(rs.getInt("NOT_DEAL_AMOUNT"));
	        	row.add(rs.getInt("ID"));
	        	lst.add(row);
	        }
	        for(int i=0; i<lst.size();i++) {
	        	int sellPrice = (int)lst.get(i).get(0);
	        	int sellDealAmount = (int)lst.get(i).get(1);
	        	int sellNotDealAmount = (int)lst.get(i).get(2);
	        	int sellEntrustmentID = (int)lst.get(i).get(3);
	        	BigDecimal sellPriceOriginal_100 = new BigDecimal(sellPrice);
	        	double sellPriceOriginal = sellPriceOriginal_100.divide(hundred).doubleValue();
	        	if(singlePrice_100 >= sellPrice) {
	        		if(not_deal_amount > sellNotDealAmount) {//主买量大于被卖量 循环不结束
	        			String sql = String.format("UPDATE ENTRUSTMENT SET DEAL_AMOUNT = '%d', NOT_DEAL_AMOUNT = '0' WHERE ID = '%d';",sellDealAmount+sellNotDealAmount,sellEntrustmentID);
	        			stmt.executeUpdate(sql);
	        			
	        			rs = stmt.executeQuery(String.format("SELECT ID, PLAYER FROM ENTRUSTMENT WHERE ID = %s LIMIT 1",sellEntrustmentID));
	        			playerName = rs.getString("PLAYER");
	        			econ.depositPlayer(playerName, sellPriceOriginal * sellNotDealAmount);
	        			rs.close();
	        			
	        			not_deal_amount = not_deal_amount - sellNotDealAmount;
	        			totalDealPrice = totalDealPrice + sellNotDealAmount * sellPrice;
	        			totalEntrustmentPrice += sellNotDealAmount * singlePrice_100;
	        		}
	        		else if(not_deal_amount == sellNotDealAmount) {//主买量等于被卖量 循环结束
	        			String sql = String.format("UPDATE ENTRUSTMENT SET DEAL_AMOUNT = '%d', NOT_DEAL_AMOUNT = '0' WHERE ID = '%d';",sellDealAmount+sellNotDealAmount,sellEntrustmentID);
	        			stmt.executeUpdate(sql);
	        			not_deal_amount = 0;
	        			sql = String.format("UPDATE ENTRUSTMENT SET DEAL_AMOUNT = '%d', NOT_DEAL_AMOUNT = '0', NOT_RECEIVE_AMOUNT = '%d' WHERE ID = '%d';", amount, amount, EntrustmentID);
	        			stmt.executeUpdate(sql);
	        			
	        			rs = stmt.executeQuery(String.format("SELECT ID, PLAYER FROM ENTRUSTMENT WHERE ID = %s LIMIT 1",sellEntrustmentID));
	        			playerName = rs.getString("PLAYER");
	        			econ.depositPlayer(playerName, sellPriceOriginal * sellNotDealAmount);
	        			rs.close();
	        			
	        			totalDealPrice = totalDealPrice + sellNotDealAmount * sellPrice;
	        			totalEntrustmentPrice += sellNotDealAmount * singlePrice_100;
	        			break;
	        		}
	        		else if(not_deal_amount < sellNotDealAmount) {//主买量小于被卖量 循环结束
	        			String sql = String.format("UPDATE ENTRUSTMENT SET DEAL_AMOUNT = '%d', NOT_DEAL_AMOUNT = '%d' WHERE ID = '%d';",sellDealAmount+not_deal_amount,sellNotDealAmount-not_deal_amount,sellEntrustmentID);
	        			stmt.executeUpdate(sql);
	        			not_deal_amount = 0;
	        			sql = String.format("UPDATE ENTRUSTMENT SET DEAL_AMOUNT = '%d', NOT_DEAL_AMOUNT = '0', NOT_RECEIVE_AMOUNT = '%d' WHERE ID = '%d';", amount, amount, EntrustmentID);
	        			stmt.executeUpdate(sql);
	        			
	        			rs = stmt.executeQuery(String.format("SELECT ID, PLAYER FROM ENTRUSTMENT WHERE ID = %s LIMIT 1",sellEntrustmentID));
	        			playerName = rs.getString("PLAYER");
	        			econ.depositPlayer(playerName, sellPriceOriginal * not_deal_amount);
	        			rs.close();
	        			
	        			totalDealPrice = totalDealPrice + not_deal_amount * sellPrice;
	        			totalEntrustmentPrice += not_deal_amount * singlePrice_100;
	        			break;
	        		}
	        	}
	        	else if(singlePrice_100 < sellPrice) {//主买价小于被卖价
	        		String sql = String.format("UPDATE ENTRUSTMENT SET DEAL_AMOUNT = '%d', NOT_DEAL_AMOUNT = '%d', NOT_RECEIVE_AMOUNT = '%d' WHERE ID = '%d';",amount - not_deal_amount, not_deal_amount, amount - not_deal_amount, EntrustmentID);
	        		stmt.executeUpdate(sql);
	        		break;
	        	}
	        }
	        if(not_deal_amount > 0) {//没有卖方
	        	String sql = String.format("UPDATE ENTRUSTMENT SET DEAL_AMOUNT = '%d', NOT_DEAL_AMOUNT = '%d', NOT_RECEIVE_AMOUNT = '%d' WHERE ID = '%d';",amount - not_deal_amount, not_deal_amount, amount - not_deal_amount, EntrustmentID);
	        	stmt.executeUpdate(sql);
	        }
	        int[] array = new int[] {totalDealPrice,totalEntrustmentPrice};
	        rs.close();
        	stmt.close();
        	c.close();
	        return array;
	    }
	    catch ( Exception e ) {
	        System.err.println( e.getClass().getName() + ": " + e.getMessage() );
	        return null;
	    }
	}

	public int[] TradeSell(String goods,int amount,int singlePrice_100,int EntrustmentID) {
		Connection c = null;
	    Statement stmt = null;
	    int totalDealPrice = 0;//总成交价
	    int totalEntrustmentPrice = 0;//总委托价
	    try {
	        Class.forName("org.sqlite.JDBC");
	        c = DriverManager.getConnection(String.format("jdbc:sqlite:%s",  getDataFolder() + File.separator + "StockShop.db"));
	        stmt = c.createStatement();
	        
	        
	        ResultSet rs = stmt.executeQuery("SELECT ID, PRICE, DEAL_AMOUNT, NOT_DEAL_AMOUNT, RECALL "+
	        								"FROM ENTRUSTMENT "+
	        								String.format("WHERE GOODS = '%s' AND TYPE = 'BUY' AND NOT_DEAL_AMOUNT <> 0 AND RECALL = 0 ",goods)+
	        								"ORDER BY PRICE DESC, ID ASC;");
	        int not_deal_amount = amount;//主卖的未交易量 
	        
	        List<List> lst = new ArrayList<>();
	        
	        while(rs.next()) {
	        	List<Integer> row = new ArrayList<>();
	        	row.add(rs.getInt("PRICE"));
	        	row.add(rs.getInt("DEAL_AMOUNT"));
	        	row.add(rs.getInt("NOT_DEAL_AMOUNT"));
	        	row.add(rs.getInt("ID"));
	        	lst.add(row);
	        }
	        for(int i=0; i<lst.size();i++) {
	        	int buyPrice = (int)lst.get(i).get(0);
	        	int buyDealAmount = (int)lst.get(i).get(1);
	        	int buyNotDealAmount = (int)lst.get(i).get(2);
	        	int buyEntrustmentID = (int)lst.get(i).get(3);
	        	if(singlePrice_100 <= buyPrice) {
	        		if(not_deal_amount > buyNotDealAmount) {
	        			String sql = String.format("UPDATE ENTRUSTMENT SET DEAL_AMOUNT = '%d', NOT_DEAL_AMOUNT = '0', NOT_RECEIVE_AMOUNT = NOT_RECEIVE_AMOUNT + %d WHERE ID = '%d';",buyDealAmount+buyNotDealAmount,buyNotDealAmount,buyEntrustmentID);
	        			stmt.executeUpdate(sql);
	        			not_deal_amount = not_deal_amount - buyNotDealAmount;
	        			totalDealPrice = totalDealPrice + buyNotDealAmount * buyPrice;
	        			totalEntrustmentPrice += buyNotDealAmount * singlePrice_100;
	        			
	        			//ItemStack stack = new ItemStack(Material.getMaterial(goods),buyNotDealAmount);
	        			
	        		}
	        		else if(not_deal_amount == buyNotDealAmount) {
	        			String sql = String.format("UPDATE ENTRUSTMENT SET DEAL_AMOUNT = '%d', NOT_DEAL_AMOUNT = '0', NOT_RECEIVE_AMOUNT = NOT_RECEIVE_AMOUNT + %d WHERE ID = '%d';",buyDealAmount+buyNotDealAmount,buyNotDealAmount,buyEntrustmentID);
	        			stmt.executeUpdate(sql);
	        			not_deal_amount = 0;
	        			sql = String.format("UPDATE ENTRUSTMENT SET DEAL_AMOUNT = '%d', NOT_DEAL_AMOUNT = '0' WHERE ID = '%d';",amount, EntrustmentID);
	        			stmt.executeUpdate(sql);
	        			totalDealPrice = totalDealPrice + buyNotDealAmount * buyPrice;
	        			totalEntrustmentPrice += buyNotDealAmount * singlePrice_100;
	        			break;
	        		}
	        		else if(not_deal_amount < buyNotDealAmount) {
	        			String sql = String.format("UPDATE ENTRUSTMENT SET DEAL_AMOUNT = '%d', NOT_DEAL_AMOUNT = '%d', NOT_RECEIVE_AMOUNT = NOT_RECEIVE_AMOUNT + %d WHERE ID = '%d';",buyDealAmount+not_deal_amount,buyNotDealAmount-not_deal_amount,not_deal_amount,buyEntrustmentID);
	        			stmt.executeUpdate(sql);
	        			not_deal_amount = 0;
	        			sql = String.format("UPDATE ENTRUSTMENT SET DEAL_AMOUNT = '%d', NOT_DEAL_AMOUNT = '0' WHERE ID = '%d';",amount, EntrustmentID);
	        			stmt.executeUpdate(sql);
	        			totalDealPrice = totalDealPrice + not_deal_amount * buyPrice;
	        			totalEntrustmentPrice += not_deal_amount * singlePrice_100;
	        			break;
	        		}
	        	}
	        	else if(singlePrice_100 > buyPrice) {
	        		String sql = String.format("UPDATE ENTRUSTMENT SET DEAL_AMOUNT = '%d', NOT_DEAL_AMOUNT = '%d' WHERE ID = '%d';",amount - not_deal_amount, not_deal_amount, EntrustmentID);
	        		stmt.executeUpdate(sql);
	        		break;
	        	}
	        }
	        if(not_deal_amount > 0) {
	        	String sql = String.format("UPDATE ENTRUSTMENT SET DEAL_AMOUNT = '%d', NOT_DEAL_AMOUNT = '%d' WHERE ID = '%d';",amount - not_deal_amount, not_deal_amount, EntrustmentID);
	        	stmt.executeUpdate(sql);
	        }
	        int[] array = new int[] {totalDealPrice,totalEntrustmentPrice};
	        rs.close();
        	stmt.close();
        	c.close();
	        return array;
	    }
	    catch ( Exception e ) {
	        System.err.println( e.getClass().getName() + ": " + e.getMessage() );
	        return null;
	    }
	}
	
	public boolean IUDatabase(String sentence) {//Insert / Update Database
		Connection c = null;
	    Statement stmt = null;
	    try {
	        Class.forName("org.sqlite.JDBC");
	        c = DriverManager.getConnection(String.format("jdbc:sqlite:%s",  getDataFolder() + File.separator + "StockShop.db"));
	        stmt = c.createStatement();
	        
	        String sql = sentence;
	        stmt.executeUpdate(sql);
	        
	        stmt.close();
	        c.close();
	        return true;
	    }
	    catch(Exception e){
	    	System.err.println( e.getClass().getName() + ": " + e.getMessage() );
	        return false;
	    }
	}
	
	public int GetNumber(String a) {//字符串中提取数字
        String regEx = "[^0-9]";
        Pattern p = Pattern.compile(regEx);
        Matcher m = p.matcher(a);
        String resString = m.replaceAll("").trim();
        int res = Integer.parseInt(resString);
        return res;
	}
	
	public List<double[]> Status(String Market) {
				String item = Market.toUpperCase();
				String key = "Market." + item + ".Enable";
				if(getConfig().contains(key)) {
					Connection c = null;
				    Statement stmt = null;
				    try {
				        Class.forName("org.sqlite.JDBC");
				        c = DriverManager.getConnection(String.format("jdbc:sqlite:%s",  getDataFolder() + File.separator + "StockShop.db"));
				        stmt = c.createStatement();
				        ResultSet rs = null;
				        
				        rs = stmt.executeQuery("SELECT GOODS, TYPE, PRICE, NOT_DEAL_AMOUNT, RECALL FROM ENTRUSTMENT "
				        		+ String.format("WHERE GOODS = '%s' AND TYPE = 'SELL' AND NOT_DEAL_AMOUNT <> 0 AND RECALL = 0 ORDER BY PRICE ASC;",item));
				        int cnt = -1;
				        double prevPrice = 0;
				        double[] priceList_s = new double[] {0,0,0,0,0};//index0 卖一...index4卖5
				        double[] amountList_s = new double[] {0,0,0,0,0};
				        while(rs.next()) {
				        	BigDecimal q_price_100 = new BigDecimal(rs.getInt("PRICE"));
				        	double q_price = q_price_100.divide(hundred).doubleValue();
				        	int q_not_deal_amount = rs.getInt("NOT_DEAL_AMOUNT");
				        	if(q_price != prevPrice) {
				        		cnt++;
				        		if(cnt == 5) break;
				        		priceList_s[cnt] = q_price;
				        		amountList_s[cnt] = q_not_deal_amount;
				        	}
				        	else if(q_price == prevPrice) {
				        		amountList_s[cnt] += q_not_deal_amount;
				        	}
				        	prevPrice = q_price;	
				        }
				        rs.close();
				        
				        rs = stmt.executeQuery("SELECT GOODS, TYPE, PRICE, NOT_DEAL_AMOUNT, RECALL FROM ENTRUSTMENT "
				        		+ String.format("WHERE GOODS = '%s' AND TYPE = 'BUY' AND NOT_DEAL_AMOUNT <> 0 AND RECALL = 0 ORDER BY PRICE DESC;",item));
				        cnt = -1;
				        prevPrice = 0;
				        double[] priceList_b = new double[] {0,0,0,0,0};//index0买一 index4买五
				        double[] amountList_b = new double[] {0,0,0,0,0};
				        while(rs.next()) {
				        	BigDecimal q_price_100 = new BigDecimal(rs.getInt("PRICE"));
				        	double q_price = q_price_100.divide(hundred).doubleValue();
				        	int q_not_deal_amount = rs.getInt("NOT_DEAL_AMOUNT");
				        	if(q_price != prevPrice) {
				        		cnt++;
				        		if(cnt == 5) break;
				        		priceList_b[cnt] = q_price;
				        		amountList_b[cnt] = q_not_deal_amount;
				        	}
				        	else if(q_price == prevPrice) {
				        		amountList_b[cnt] += q_not_deal_amount;
				        	}
				        	prevPrice = q_price;	
				        }
				        rs.close();
				        stmt.close();
				        c.close();
				        List <double[]> list = null;
				        list.add(priceList_s);
				        list.add(amountList_s);
				        list.add(priceList_b);
				        list.add(amountList_b);
				        return list;
				    }
				    catch ( Exception e ) {
				        System.err.println( e.getClass().getName() + ": " + e.getMessage() );
				        return null;
				    }
				    
				}
				else if(!getConfig().contains(key)) {//未找到该市场
					return null;
				}
				return null;
			}
	
	@EventHandler
	public void onInventoryClickEvent(InventoryClickEvent e) {
		Player p = (Player) e.getWhoClicked();
		if(e.getRawSlot()<0 || e.getRawSlot()>e.getInventory().getSize() || e.getInventory() == null) {
			return;
		}
		else if(e.getWhoClicked().getOpenInventory().getTitle().contains("StockShop 全部市场 - 第")) {
			e.setCancelled(true);
			int slot = e.getRawSlot();
			int page = GetNumber(e.getWhoClicked().getOpenInventory().getTitle());
			String item = e.getCurrentItem().getType().toString();
			
			if(item.equals("AIR")) {
				return;
			}
			else if(slot>=45 && slot<=53) {
				if(slot == 48) {
					p.closeInventory();
					p.performCommand(String.format("ss all %s",page-1));
				}
				else if(slot == 50) {
					p.closeInventory();
					p.performCommand(String.format("ss all %s",page+1));
				}
			}
			else{
				p.closeInventory();
				p.performCommand(String.format("ss gui status %s",item));
			}
		}
		else if(e.getWhoClicked().getOpenInventory().getTitle().equals("StockShop 主菜单")) {
			e.setCancelled(true);
			if(e.getRawSlot() == 0) {
				p.closeInventory();
				p.performCommand("ss gui all");
			}
		}
		return;
	}
}
