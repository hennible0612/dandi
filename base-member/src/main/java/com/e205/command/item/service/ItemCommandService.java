package com.e205.command.item.service;

import com.e205.command.item.command.CreateItemCommand;
import com.e205.command.item.command.DeleteItemCommand;
import com.e205.command.item.command.UpdateItemCommand;
import com.e205.command.item.command.UpdateItemOrderCommand;

public interface ItemCommandService {

  void save(CreateItemCommand createItemCommand);

  void update(UpdateItemCommand updateCommand);

  void updateItemOrder(UpdateItemOrderCommand updateItemOrderCommand);

  void delete(DeleteItemCommand deleteItemCommand);
}