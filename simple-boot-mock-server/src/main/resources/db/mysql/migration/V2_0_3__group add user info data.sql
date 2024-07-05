update t_mock_group
set user_name='admin'
where user_name is null
   or user_name = '';
